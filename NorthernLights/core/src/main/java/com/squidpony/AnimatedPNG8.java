package com.squidpony;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ByteArray;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.StreamUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Indexed-mode AnimatedPNG encoder with compression. An instance can be reused to encode multiple APNGs with low allocation.
 * You can configure the target palette and how this can dither colors via the {@link #palette} field, which is a
 * {@link PaletteReducer} object that is allowed to be null and can be reused. The methods
 * {@link PaletteReducer#exact(Color[])} or {@link PaletteReducer#analyze(Pixmap)} can be used to make the target
 * palette match a specific set of colors or the colors in an existing image. You can use
 * {@link PaletteReducer#setDitherStrength(float)} to reduce (or increase) dither strength; the algorithm used here is
 * standard Floyd-Steinberg dithering.
 * <p>
 * From LibGDX in the class PixmapIO, with modifications to support indexed-mode files, dithering, and other features.
 * <pre>
 * Copyright (c) 2007 Matthias Mann - www.matthiasmann.de
 * Copyright (c) 2014 Nathan Sweet
 * Copyright (c) 2018 Tommy Ettinger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * </pre>
 *
 * @author Matthias Mann
 * @author Nathan Sweet
 * @author Tommy Ettinger (PNG-8 parts only)
 */

public class AnimatedPNG8 implements Disposable {
    static private final byte[] SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    static private final int IHDR = 0x49484452, IDAT = 0x49444154, IEND = 0x49454E44,
            PLTE = 0x504C5445, TRNS = 0x74524E53,
            acTL = 0x6163544C, fcTL = 0x6663544C, fdAT = 0x66644154;

    static private final byte COLOR_INDEXED = 3;
    static private final byte COMPRESSION_DEFLATE = 0;
    static private final byte FILTER_NONE = 0;
    static private final byte INTERLACE_NONE = 0;
    static private final byte PAETH = 4;

    private final ChunkBuffer buffer;
    private final Deflater deflater;
    private ByteArray lineOutBytes, curLineBytes, prevLineBytes;
    private boolean flipY = true;
    private int lastLineLen;

    public PaletteReducer palette;

    public AnimatedPNG8 () {
        this(128 * 128);
    }

    public AnimatedPNG8 (int initialBufferSize) {
        buffer = new ChunkBuffer(initialBufferSize);
        deflater = new Deflater();
    }

    /**
     * If true, the resulting PNG is flipped vertically. Default is true.
     */
    public void setFlipY(boolean flipY) {
        this.flipY = flipY;
    }

    /**
     * Sets the deflate compression level. Default is {@link Deflater#DEFAULT_COMPRESSION}.
     */
    public void setCompression(int level) {
        deflater.setLevel(level);
    }

    /**
     * Writes the given Pixmap to the requested FileHandle, computing an 8-bit palette from the most common colors in
     * pixmap. If there are 256 or less colors and none are transparent, this will use 256 colors in its palette exactly
     * with no transparent entry, but if there are more than 256 colors or any are transparent, then one color will be
     * used for "fully transparent" and 255 opaque colors will be used.
     *
     * @param file   a FileHandle that must be writable, and will have the given Pixmap written as a PNG-8 image
     * @param frames a Pixmap Array to write as a sequence of frames to the given output stream
     * @param fps    how many frames per second the animation should run at
     * @throws IOException if file writing fails for any reason
     */
    public void write(FileHandle file, Array<Pixmap> frames, int fps) throws IOException {
        write(file, frames, fps, true);
    }

    /**
     * Writes the pixmap to the stream without closing the stream, optionally computing an 8-bit palette from the given
     * Pixmap. If {@link #palette} is null (the default unless it has been assigned a PaletteReducer value), this will
     * compute a palette from the given Pixmap regardless of computePalette. Optionally dithers the result if
     * {@code dither} is true.
     *
     * @param file   a FileHandle that must be writable, and will have the given Pixmap written as a PNG-8 image
     * @param frames a Pixmap Array to write as a sequence of frames to the given output stream
     * @param fps    how many frames per second the animation should run at
     * @param dither true if this should dither colors that can't be represented exactly
     * @throws IOException if file writing fails for any reason
     */
    public void write(FileHandle file, Array<Pixmap> frames, int fps, boolean dither) throws IOException {
        OutputStream output = file.write(false);
        try {
            write(output, frames, fps, dither);
        } finally {
            StreamUtils.closeQuietly(output);
        }
    }

    /**
     * Writes the pixmap to the stream without closing the stream, optionally computing an 8-bit palette from the given
     * Pixmap. If {@link #palette} is null (the default unless it has been assigned a PaletteReducer value), this will
     * compute a palette from the given Pixmap regardless of computePalette.
     *
     * @param output an OutputStream that will not be closed
     * @param frames a Pixmap Array to write as a sequence of frames to the given output stream
     * @param fps    how many frames per second the animation should run at
     * @param dither true if this should dither colors that can't be represented exactly
     */
    public void write(OutputStream output, Array<Pixmap> frames, int fps, boolean dither) throws IOException {
        if (palette == null)
            palette = new PaletteReducer();
        if (dither)
            writeDithered(output, frames, fps);
        else
            writeSolid(output, frames, fps);
    }

    private void writeSolid(OutputStream output, Array<Pixmap> frames, int fps) throws IOException {
        Pixmap pixmap = frames.first();
        final int[] paletteArray = palette.paletteArray;
        final byte[] paletteMapping = palette.paletteMapping;

        DeflaterOutputStream deflaterOutput = new DeflaterOutputStream(buffer, deflater);
        DataOutputStream dataOutput = new DataOutputStream(output);
        dataOutput.write(SIGNATURE);

        final int width = pixmap.getWidth();
        final int height = pixmap.getHeight();

        buffer.writeInt(IHDR);
        buffer.writeInt(width);
        buffer.writeInt(height);
        buffer.writeByte(8); // 8 bits per component.
        buffer.writeByte(COLOR_INDEXED);
        buffer.writeByte(COMPRESSION_DEFLATE);
        buffer.writeByte(FILTER_NONE);
        buffer.writeByte(INTERLACE_NONE);
        buffer.endChunk(dataOutput);

        buffer.writeInt(PLTE);
        for (int i = 0; i < paletteArray.length; i++) {
            int p = paletteArray[i];
            buffer.write(p >>> 24);
            buffer.write(p >>> 16);
            buffer.write(p >>> 8);
        }
        buffer.endChunk(dataOutput);

        boolean hasTransparent = false;
        if (paletteArray[0] == 0) {
            hasTransparent = true;
            buffer.writeInt(TRNS);
            buffer.write(0);
            buffer.endChunk(dataOutput);
        }
        buffer.writeInt(acTL);
        buffer.writeInt(frames.size);
        buffer.writeInt(0);
        buffer.endChunk(dataOutput);

        byte[] lineOut, curLine, prevLine;
        int color;
        int seq = 0;
        for (int i = 0; i < frames.size; i++) {

            buffer.writeInt(fcTL);
            buffer.writeInt(seq++);
            buffer.writeInt(width);
            buffer.writeInt(height);
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeShort(1);
            buffer.writeShort(fps);
            buffer.writeByte(0);
            buffer.writeByte(0);
            buffer.endChunk(dataOutput);

            if (i == 0) {
                buffer.writeInt(IDAT);
            } else {
                pixmap = frames.get(i);
                buffer.writeInt(fdAT);
                buffer.writeInt(seq++);
            }
            deflater.reset();

            if (lineOutBytes == null) {
                lineOut = (lineOutBytes = new ByteArray(width)).items;
                curLine = (curLineBytes = new ByteArray(width)).items;
                prevLine = (prevLineBytes = new ByteArray(width)).items;
            } else {
                lineOut = lineOutBytes.ensureCapacity(width);
                curLine = curLineBytes.ensureCapacity(width);
                prevLine = prevLineBytes.ensureCapacity(width);
                for (int ln = 0, n = lastLineLen; ln < n; ln++)
                    prevLine[ln] = 0;
            }
            lastLineLen = width;

            for (int y = 0; y < height; y++) {
                int py = flipY ? (height - y - 1) : y;
                for (int px = 0; px < width; px++) {
                    color = pixmap.getPixel(px, py);
                    if ((color & 0x80) == 0 && hasTransparent)
                        curLine[px] = 0;
                    else {
                        int rr = ((color >>> 24));
                        int gg = ((color >>> 16) & 0xFF);
                        int bb = ((color >>> 8) & 0xFF);
                        curLine[px] = paletteMapping[((rr << 7) & 0x7C00)
                                | ((gg << 2) & 0x3E0)
                                | ((bb >>> 3))];
                    }
                }

                lineOut[0] = (byte) (curLine[0] - prevLine[0]);

                //Paeth
                for (int x = 1; x < width; x++) {
                    int a = curLine[x - 1] & 0xff;
                    int b = prevLine[x] & 0xff;
                    int c = prevLine[x - 1] & 0xff;
                    int p = a + b - c;
                    int pa = p - a;
                    if (pa < 0) pa = -pa;
                    int pb = p - b;
                    if (pb < 0) pb = -pb;
                    int pc = p - c;
                    if (pc < 0) pc = -pc;
                    if (pa <= pb && pa <= pc)
                        c = a;
                    else if (pb <= pc)
                        c = b;
                    lineOut[x] = (byte) (curLine[x] - c);
                }

                deflaterOutput.write(PAETH);
                deflaterOutput.write(lineOut, 0, width);

                byte[] temp = curLine;
                curLine = prevLine;
                prevLine = temp;
            }
            deflaterOutput.finish();
            buffer.endChunk(dataOutput);
        }

        buffer.writeInt(IEND);
        buffer.endChunk(dataOutput);

        output.flush();
    }

    private void writeDithered(OutputStream output, Array<Pixmap> frames, int fps) throws IOException {
        Pixmap pixmap = frames.first();
        final int[] paletteArray = palette.paletteArray;
        final byte[] paletteMapping = palette.paletteMapping;

        DeflaterOutputStream deflaterOutput = new DeflaterOutputStream(buffer, deflater);
        DataOutputStream dataOutput = new DataOutputStream(output);
        dataOutput.write(SIGNATURE);

        final int width = pixmap.getWidth();
        final int height = pixmap.getHeight();

        buffer.writeInt(IHDR);
        buffer.writeInt(width);
        buffer.writeInt(height);
        buffer.writeByte(8); // 8 bits per component.
        buffer.writeByte(COLOR_INDEXED);
        buffer.writeByte(COMPRESSION_DEFLATE);
        buffer.writeByte(FILTER_NONE);
        buffer.writeByte(INTERLACE_NONE);
        buffer.endChunk(dataOutput);

        buffer.writeInt(PLTE);
        for (int i = 0; i < paletteArray.length; i++) {
            int p = paletteArray[i];
            buffer.write(p >>> 24);
            buffer.write(p >>> 16);
            buffer.write(p >>> 8);
        }
        buffer.endChunk(dataOutput);

        boolean hasTransparent = false;
        if (paletteArray[0] == 0) {
            hasTransparent = true;
            buffer.writeInt(TRNS);
            buffer.write(0);
            buffer.endChunk(dataOutput);
        }
        buffer.writeInt(acTL);
        buffer.writeInt(frames.size);
        buffer.writeInt(0);
        buffer.endChunk(dataOutput);

        byte[] lineOut, curLine, prevLine;
        byte[] curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue;
        int color;
        if (palette.curErrorRedBytes == null) {
            curErrorRed = (palette.curErrorRedBytes = new ByteArray(width)).items;
            nextErrorRed = (palette.nextErrorRedBytes = new ByteArray(width)).items;
            curErrorGreen = (palette.curErrorGreenBytes = new ByteArray(width)).items;
            nextErrorGreen = (palette.nextErrorGreenBytes = new ByteArray(width)).items;
            curErrorBlue = (palette.curErrorBlueBytes = new ByteArray(width)).items;
            nextErrorBlue = (palette.nextErrorBlueBytes = new ByteArray(width)).items;
        } else {
            curErrorRed = palette.curErrorRedBytes.ensureCapacity(width);
            nextErrorRed = palette.nextErrorRedBytes.ensureCapacity(width);
            curErrorGreen = palette.curErrorGreenBytes.ensureCapacity(width);
            nextErrorGreen = palette.nextErrorGreenBytes.ensureCapacity(width);
            curErrorBlue = palette.curErrorBlueBytes.ensureCapacity(width);
            nextErrorBlue = palette.nextErrorBlueBytes.ensureCapacity(width);
            for (int i = 0; i < width; i++) {
                nextErrorRed[i] = 0;
                nextErrorGreen[i] = 0;
                nextErrorBlue[i] = 0;
            }
        }

        lastLineLen = width;

        int used, rdiff, gdiff, bdiff;
        byte er, eg, eb, paletteIndex;
        float w1 = palette.ditherStrength * 0.125f, w3 = w1 * 3f, w5 = w1 * 5f, w7 = w1 * 7f;

        int seq = 0;
        for (int i = 0; i < frames.size; i++) {

            buffer.writeInt(fcTL);
            buffer.writeInt(seq++);
            buffer.writeInt(width);
            buffer.writeInt(height);
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeShort(1);
            buffer.writeShort(fps);
            buffer.writeByte(0);
            buffer.writeByte(0);
            buffer.endChunk(dataOutput);

            if (i == 0) {
                buffer.writeInt(IDAT);
            } else {
                pixmap = frames.get(i);
                buffer.writeInt(fdAT);
                buffer.writeInt(seq++);
            }
            deflater.reset();

            if (lineOutBytes == null) {
                lineOut = (lineOutBytes = new ByteArray(width)).items;
                curLine = (curLineBytes = new ByteArray(width)).items;
                prevLine = (prevLineBytes = new ByteArray(width)).items;
            } else {
                lineOut = lineOutBytes.ensureCapacity(width);
                curLine = curLineBytes.ensureCapacity(width);
                prevLine = prevLineBytes.ensureCapacity(width);
                for (int ln = 0, n = lastLineLen; ln < n; ln++)
                    prevLine[ln] = 0;
            }
            lastLineLen = width;

            for (int y = 0; y < height; y++) {
                int py = flipY ? (height - y - 1) : y;
                int ny = flipY ? (height - y - 2) : y + 1;
                for (int x = 0; x < width; x++) {
                    curErrorRed[x] = nextErrorRed[x];
                    curErrorGreen[x] = nextErrorGreen[x];
                    curErrorBlue[x] = nextErrorBlue[x];
                    nextErrorRed[x] = 0;
                    nextErrorGreen[x] = 0;
                    nextErrorBlue[x] = 0;
                }
                for (int px = 0; px < width; px++) {
                    color = pixmap.getPixel(px, py) & 0xF8F8F880;
                    if ((color & 0x80) == 0 && hasTransparent)
                        curLine[px] = 0;
                    else {
                        er = curErrorRed[px];
                        eg = curErrorGreen[px];
                        eb = curErrorBlue[px];
                        color |= (color >>> 5 & 0x07070700) | 0xFE;
                        int rr = MathUtils.clamp(((color >>> 24)) + (er), 0, 0xFF);
                        int gg = MathUtils.clamp(((color >>> 16) & 0xFF) + (eg), 0, 0xFF);
                        int bb = MathUtils.clamp(((color >>> 8) & 0xFF) + (eb), 0, 0xFF);
                        curLine[px] = paletteIndex =
                                paletteMapping[((rr << 7) & 0x7C00)
                                        | ((gg << 2) & 0x3E0)
                                        | ((bb >>> 3))];
                        used = paletteArray[paletteIndex & 0xFF];
                        rdiff = (color >>> 24) - (used >>> 24);
                        gdiff = (color >>> 16 & 255) - (used >>> 16 & 255);
                        bdiff = (color >>> 8 & 255) - (used >>> 8 & 255);
                        if (px < width - 1) {
                            curErrorRed[px + 1] += rdiff * w7;
                            curErrorGreen[px + 1] += gdiff * w7;
                            curErrorBlue[px + 1] += bdiff * w7;
                        }
                        if (ny < height) {
                            if (px > 0) {
                                nextErrorRed[px - 1] += rdiff * w3;
                                nextErrorGreen[px - 1] += gdiff * w3;
                                nextErrorBlue[px - 1] += bdiff * w3;
                            }
                            if (px < width - 1) {
                                nextErrorRed[px + 1] += rdiff * w1;
                                nextErrorGreen[px + 1] += gdiff * w1;
                                nextErrorBlue[px + 1] += bdiff * w1;
                            }
                            nextErrorRed[px] += rdiff * w5;
                            nextErrorGreen[px] += gdiff * w5;
                            nextErrorBlue[px] += bdiff * w5;
                        }
                    }
                }
                lineOut[0] = (byte) (curLine[0] - prevLine[0]);

                //Paeth
                for (int x = 1; x < width; x++) {
                    int a = curLine[x - 1] & 0xff;
                    int b = prevLine[x] & 0xff;
                    int c = prevLine[x - 1] & 0xff;
                    int p = a + b - c;
                    int pa = p - a;
                    if (pa < 0) pa = -pa;
                    int pb = p - b;
                    if (pb < 0) pb = -pb;
                    int pc = p - c;
                    if (pc < 0) pc = -pc;
                    if (pa <= pb && pa <= pc)
                        c = a;
                    else if (pb <= pc)
                        c = b;
                    lineOut[x] = (byte) (curLine[x] - c);
                }

                deflaterOutput.write(PAETH);
                deflaterOutput.write(lineOut, 0, width);

                byte[] temp = curLine;
                curLine = prevLine;
                prevLine = temp;
            }
            deflaterOutput.finish();
            buffer.endChunk(dataOutput);
        }

        buffer.writeInt(IEND);
        buffer.endChunk(dataOutput);

        output.flush();
    }

    /**
     * Disposal will happen automatically in {@link #finalize()} but can be done explicitly if desired.
     */
    public void dispose() {
        deflater.end();
    }
    
    /**
     * Simple PNG IO from https://www.java-tips.org/java-se-tips-100019/23-java-awt-image/2283-png-file-format-decoder-in-java.html .
     *
     * @param inStream
     * @return
     * @throws IOException
     */
    protected static LinkedHashMap<String, byte[]> readChunks(InputStream inStream) throws IOException {
        DataInputStream in = new DataInputStream(inStream);
        if (in.readLong() != 0x89504e470d0a1a0aL)
            throw new IOException("PNG signature not found!");
        LinkedHashMap<String, byte[]> chunks = new LinkedHashMap<>(10);
        boolean trucking = true;
        while (trucking) {
            try {
                // Read the length.
                int length = in.readInt();
                if (length < 0)
                    throw new IOException("Sorry, that file is too long.");
                // Read the type.
                byte[] typeBytes = new byte[4];
                in.readFully(typeBytes);
                // Read the data.
                byte[] data = new byte[length];
                in.readFully(data);
                // Read the CRC, discard it.
                int crc = in.readInt();
                String type = new String(typeBytes, "UTF8");
                chunks.put(type, data);
            } catch (EOFException eofe) {
                trucking = false;
            }
        }
        in.close();
        return chunks;
    }

    /**
     * Simple PNG IO from https://www.java-tips.org/java-se-tips-100019/23-java-awt-image/2283-png-file-format-decoder-in-java.html .
     *
     * @param outStream
     * @param chunks
     */
    protected static void writeChunks(OutputStream outStream, LinkedHashMap<String, byte[]> chunks) {
        DataOutputStream out = new DataOutputStream(outStream);
        CRC32 crc = new CRC32();
        try {
            out.writeLong(0x89504e470d0a1a0aL);
            for (HashMap.Entry<String, byte[]> ent : chunks.entrySet()) {
                out.writeInt(ent.getValue().length);
                out.writeBytes(ent.getKey());
                byte[] bytes = ent.getKey().getBytes(StandardCharsets.UTF_8);
                crc.update(bytes, 0, bytes.length);
                out.write(ent.getValue());
                crc.update(ent.getValue(), 0, ent.getValue().length);
                out.writeInt((int) crc.getValue());
                crc.reset();
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Given a FileHandle to read from and a FileHandle to write to, duplicates the input FileHandle and changes its
     * palette (in full and in order) to exactly match {@code palette}. This is only likely to work if the input file
     * was written with the same palette order.
     *
     * @param input   FileHandle to read from that should have a similar palette (and very similar order) to {@code palette}
     * @param output  FileHandle that should be writable and empty
     * @param palette RGBA8888 color array
     */
    public static void swapPalette(FileHandle input, FileHandle output, int[] palette) {
        try {
            InputStream inputStream = input.read();
            LinkedHashMap<String, byte[]> chunks = readChunks(inputStream);
            byte[] pal = chunks.get("PLTE");
            if (pal == null) {
                output.write(inputStream, false);
                return;
            }
            for (int i = 0, p = 0; i < palette.length && p < pal.length - 2; i++) {
                int rgba = palette[i];
                pal[p++] = (byte) (rgba >>> 24);
                pal[p++] = (byte) (rgba >>> 16);
                pal[p++] = (byte) (rgba >>> 8);
            }
            writeChunks(output.write(false), chunks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copied straight out of libGDX, in the PixmapIO class.
     */
    static class ChunkBuffer extends DataOutputStream {
        final ByteArrayOutputStream buffer;
        final CRC32 crc;

        ChunkBuffer (int initialSize) {
            this(new ByteArrayOutputStream(initialSize), new CRC32());
        }

        private ChunkBuffer (ByteArrayOutputStream buffer, CRC32 crc) {
            super(new CheckedOutputStream(buffer, crc));
            this.buffer = buffer;
            this.crc = crc;
        }

        public void endChunk (DataOutputStream target) throws IOException {
            flush();
            target.writeInt(buffer.size() - 4);
            buffer.writeTo(target);
            target.writeInt((int)crc.getValue());
            buffer.reset();
            crc.reset();
        }
    }
}
