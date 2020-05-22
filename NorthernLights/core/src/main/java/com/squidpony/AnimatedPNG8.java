package com.squidpony;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ByteArray;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.StreamUtils;
import com.github.tommyettinger.bluegrass.BlueNoise;

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
     * Writes the given Pixmaps to the requested FileHandle at the requested frames per second.
     * If {@link #palette} is null (the default unless it has been assigned a PaletteReducer value), this will
     * compute a palette from all of the frames given. Otherwise, this uses the colors already in {@link #palette}.
     * Always dithers the result.
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
     * Writes the Pixmaps to the stream without closing the stream, optionally computing an 8-bit palette from the given
     * Pixmap. If {@link #palette} is null (the default unless it has been assigned a PaletteReducer value), this will
     * compute a palette from all of the frames given. Otherwise, this uses the colors already in {@link #palette}.
     * Optionally dithers the result if {@code dither} is true.
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
     * Writes the Pixmaps to the stream without closing the stream, optionally computing an 8-bit palette from the given
     * Pixmaps. If {@link #palette} is null (the default unless it has been assigned a PaletteReducer value), this will
     * compute a palette from all of the frames given. Otherwise, this uses the colors already in {@link #palette}.
     * Optionally dithers the result if {@code dither} is true.
     * 
     * @param output an OutputStream that will not be closed
     * @param frames a Pixmap Array to write as a sequence of frames to the given output stream
     * @param fps    how many frames per second the animation should run at
     * @param dither true if this should dither colors that can't be represented exactly
     */
    public void write(OutputStream output, Array<Pixmap> frames, int fps, boolean dither) throws IOException {
        if (palette == null)
            palette = new PaletteReducer(frames);
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
    /**
     * Inverse cosine function (arccos) but with output measured in turns instead of radians. Possible results for this
     * range from 0.0f (inclusive) to 0.5f (inclusive).
     * <br>
     * This method is extremely similar to the non-turn approximation.
     * @param n a float from -1.0f to 1.0f (both inclusive), usually the output of sin_() or cos_()
     * @return one of the values that would produce {@code n} if it were adjusted to 1/2pi range and passed to cos() 
     */
    private static float acos_(final float n)
    {
        final float ax = Math.abs(n), ay = (float) Math.sqrt(1f - n * n);
        if(ax < ay)
        {
            final float a = ax / ay, s = a * a,
                    r = 0.25f - (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a) * 0.15915494309189535f;
            return (n < 0.0f) ? 0.5f - r : r;
        }
        else {
            final float a = ay / ax, s = a * a,
                    r = (((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a) * 0.15915494309189535f;
            return (n < 0.0f) ? 0.5f - r : r;
        }
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
        int color, used;

        lastLineLen = width;

        byte paletteIndex;
        float pos, adj;
        final float strength = palette.ditherStrength * 4f;

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
                    color = pixmap.getPixel(px, py) & 0xF8F8F880;
                    if ((color & 0x80) == 0 && hasTransparent)
                        curLine[px] = 0;
                    else {
                        color |= (color >>> 5 & 0x07070700) | 0xFE;
                        int rr = ((color >>> 24)       );
                        int gg = ((color >>> 16) & 0xFF);
                        int bb = ((color >>> 8)  & 0xFF);
                        paletteIndex =
                                paletteMapping[((rr << 7) & 0x7C00)
                                        | ((gg << 2) & 0x3E0)
                                        | ((bb >>> 3))];
                        used = paletteArray[paletteIndex & 0xFF];
                    adj = (-0.25f + acos_((BlueNoise.getChosen(px, y, i * 31) + 0.5f) * 0.00784313725490196f)
                            + (BlueNoise.get(~px, ~y, ~i) + 0.5f) * 0.0019607844f) * strength;
                        
//                        pos = (px * 0.06711056f + y * 0.00583715f);
//                        pos -= (int)pos;
//                        pos *= 52.9829189f;
//                        pos -= (int)pos;
//                        adj = ((float)Math.sqrt(pos) * pos - 0.25f) * strength;
                        //pos = (BlueNoise.get(y, px, ~i) + 0.5f) * 0.00784313725490196f; // -1f to 1f
                        rr = MathUtils.clamp((int) (rr + (adj * (0.5f + rr - (used >>> 24       )))), 0, 0xFF);
                        gg = MathUtils.clamp((int) (gg + (adj * (0.5f + gg - (used >>> 16 & 0xFF)))), 0, 0xFF);
                        bb = MathUtils.clamp((int) (bb + (adj * (0.5f + bb - (used >>> 8  & 0xFF)))), 0, 0xFF);
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
