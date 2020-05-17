package com.squidpony;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StreamUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Based on Nick Badal's Android port ( https://github.com/nbadal/android-gif-encoder/blob/master/GifEncoder.java ) of
 * Alessandro La Rossa's J2ME port ( http://www.jappit.com/blog/2008/12/04/j2me-animated-gif-encoder/ ) of this pure
 * Java animated GIF encoder by Kevin Weiner ( http://www.java2s.com/Code/Java/2D-Graphics-GUI/AnimatedGifEncoder.htm ).
 * The original has no copyright asserted, so this file continues that tradition and does not assert copyright either.
 */
public class AnimatedGif {
    public void write(FileHandle file, Array<Pixmap> frames) throws IOException {
        write(file, frames, 30);
    }
    public void write(FileHandle file, Array<Pixmap> frames, int fps) throws IOException {
        OutputStream output = file.write(false);
        try {
            write(output, frames, fps);
        } finally {
            StreamUtils.closeQuietly(output);
        }
    }

    private void write(OutputStream output, Array<Pixmap> frames, int fps) throws IOException {
        if (palette == null)
            palette = new PaletteReducer();
        start(output);
        setFrameRate(fps);
        for (int i = 0; i < frames.size; i++) {
            addFrame(frames.get(i));
        }
        finish();
    }
    
    protected int width; // image size

    protected int height;

    protected int x = 0;

    protected int y = 0;

    protected int transIndex = -1; // transparent index in color table

    protected int repeat = 0; // loop repeat

    protected int delay = 16; // frame delay (hundredths)

    protected boolean started = false; // ready to output frames

    protected OutputStream out;

    protected Pixmap image; // current frame

    protected byte[] pixels; // BGR byte array from frame

    protected byte[] indexedPixels; // converted frame indexed to palette

    protected int colorDepth; // number of bit planes

    protected byte[] colorTab; // RGB palette

    protected boolean[] usedEntry = new boolean[256]; // active palette entries

    protected int palSize = 7; // color table size (bits-1)

    protected int dispose = -1; // disposal code (-1 = use default)

    protected boolean closeStream = false; // close stream when finished

    protected boolean firstFrame = true;

    protected boolean sizeSet = false; // if false, get size from first frame

    public PaletteReducer palette;

    protected float strength = 0.5f;

    /**
     * Sets the delay time between each frame, or changes it for subsequent frames
     * (applies to last frame added).
     *
     * @param ms int delay time in milliseconds
     */
    public void setDelay(int ms) {
        delay = ms;
    }

    /**
     * Sets the GIF frame disposal code for the last added frame and any
     * subsequent frames. Default is 0 if no transparent color has been set,
     * otherwise 2.
     *
     * @param code int disposal code.
     */
    public void setDispose(int code) {
        if (code >= 0) {
            dispose = code;
        }
    }

    /**
     * Sets the number of times the set of GIF frames should be played. Default is
     * 1; 0 means play indefinitely. Must be invoked before the first image is
     * added.
     *
     * @param iter int number of iterations.
     * @return
     */
    public void setRepeat(int iter) {
        if (iter >= 0) {
            repeat = iter;
        }
    }

    /**
     * Adds next GIF frame. The frame is not written immediately, but is actually
     * deferred until the next frame is received so that timing data can be
     * inserted. Invoking <code>finish()</code> flushes all frames. If
     * <code>setSize</code> was not invoked, the size of the first image is used
     * for all subsequent frames.
     *
     * @param im BufferedImage containing frame to write.
     * @return true if successful.
     */
    public boolean addFrame(Pixmap im) {
        if ((im == null) || !started) {
            return false;
        }
        boolean ok = true;
        try {
            if (!sizeSet) {
                // use first frame's size
                setSize(im.getWidth(), im.getHeight());
            }
            image = im;
            getImagePixels(); // convert to correct format if necessary
            analyzePixels(); // build color table & map pixels
            if (firstFrame) {
                writeLSD(); // logical screen descriptior
                writePalette(); // global color table
                if (repeat >= 0) {
                    // use NS app extension to indicate reps
                    writeNetscapeExt();
                }
            }
            writeGraphicCtrlExt(); // write graphic control extension
            writeImageDesc(); // image descriptor
            if (!firstFrame) {
                writePalette(); // local color table
            }
            writePixels(); // encode and write pixel data
            firstFrame = false;
        } catch (IOException e) {
            ok = false;
        }

        return ok;
    }

    /**
     * Flushes any pending data and closes output file. If writing to an
     * OutputStream, the stream is not closed.
     */
    public boolean finish() {
        if (!started)
            return false;
        boolean ok = true;
        started = false;
        try {
            out.write(0x3b); // gif trailer
            out.flush();
            if (closeStream) {
                out.close();
            }
        } catch (IOException e) {
            ok = false;
        }

        // reset for subsequent use
        transIndex = -1;
        out = null;
        image = null;
        pixels = null;
        indexedPixels = null;
        colorTab = null;
        closeStream = false;
        firstFrame = true;

        return ok;
    }

    /**
     * Sets frame rate in frames per second. Equivalent to
     * <code>setDelay(1000.0f / fps)</code>.
     *
     * @param fps float frame rate (frames per second)
     */
    public void setFrameRate(float fps) {
        if (fps != 0f) {
            delay = (int) (1000f / fps);
        }
    }

    /**
     * Sets the GIF frame size. The default size is the size of the first frame
     * added if this method is not invoked.
     *
     * @param w int frame width.
     * @param h int frame width.
     */
    public void setSize(int w, int h) {
        width = w;
        height = h;
        if (width < 1)
            width = 320;
        if (height < 1)
            height = 240;
        sizeSet = true;
    }

    /**
     * Sets the GIF frame position. The position is 0,0 by default.
     * Useful for only updating a section of the image
     *
     * @param x int frame x position.
     * @param y int frame y position.
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Initiates GIF file creation on the given stream. The stream is not closed
     * automatically.
     *
     * @param os OutputStream on which GIF images are written.
     * @return false if initial write failed.
     */
    public boolean start(OutputStream os) {
        if (os == null)
            return false;
        boolean ok = true;
        closeStream = false;
        out = os;
        try {
            writeString("GIF89a"); // header
        } catch (IOException e) {
            ok = false;
        }
        return started = ok;
    }

    /**
     * Analyzes image colors and creates color map.
     */
    protected void analyzePixels() {
        int nPix = width * height;
        indexedPixels = new byte[nPix];
        palette.analyze(image);
        final int[] paletteArray = palette.paletteArray;
        final byte[] paletteMapping = palette.paletteMapping;
        // initialize quantizer
        colorTab = new byte[256 * 3]; // create reduced palette
        for (int i = 0, bi = 0; i < 256; i++) {
            int pa = paletteArray[i];
            colorTab[bi++] = (byte) (pa >>> 24);
            colorTab[bi++] = (byte) (pa >>> 16);
            colorTab[bi++] = (byte) (pa >>> 8);
            usedEntry[i] = false;
        }
        // map image pixels to new palette
        int color, used;
        boolean hasTransparent = paletteArray[0] == 0;
        float pos, adj, strength = this.strength;
        for (int y = 0, i = 0; y < height && i < nPix; y++) {
            for (int px = 0; px < width & i < nPix; px++) {
                color = image.getPixel(px, y) & 0xF8F8F880;
                if ((color & 0x80) == 0 && hasTransparent)
                    indexedPixels[i++] = 0;
                else {
                    color |= (color >>> 5 & 0x07070700) | 0xFE;
                    int rr = ((color >>> 24));
                    int gg = ((color >>> 16) & 0xFF);
                    int bb = ((color >>> 8) & 0xFF);
                    used = paletteArray[paletteMapping[((rr << 7) & 0x7C00)
                            | ((gg << 2) & 0x3E0)
                            | ((bb >>> 3))] & 0xFF];
                    pos = (px * 0.06711056f + y * 0.00583715f);
                    pos -= (int) pos;
                    pos *= 52.9829189f;
                    pos -= (int) pos;
                    adj = ((float) Math.sqrt(pos) * pos - 0.3125f) * strength;
                    rr = MathUtils.clamp((int) (rr + (adj * ((rr - (used >>> 24))))), 0, 0xFF);
                    gg = MathUtils.clamp((int) (gg + (adj * ((gg - (used >>> 16 & 0xFF))))), 0, 0xFF);
                    bb = MathUtils.clamp((int) (bb + (adj * ((bb - (used >>> 8 & 0xFF))))), 0, 0xFF);
                    usedEntry[(indexedPixels[i] = paletteMapping[((rr << 7) & 0x7C00)
                            | ((gg << 2) & 0x3E0)
                            | ((bb >>> 3))]) & 255] = true;
                    i++;
                }
            }
        }
        pixels = null;
        colorDepth = 8;
        palSize = 7;
        // get closest match to transparent color if specified
        if (hasTransparent) {
            transIndex = 0;
        }
    }

    /**
     * Extracts image pixels into byte array "pixels"
     */
    protected void getImagePixels() {
        int w = image.getWidth();
        int h = image.getHeight();
        if ((w != width) || (h != height)) {
            // create new image with right size/format
            Pixmap temp = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            temp.drawPixmap(image, 0, 0);
            image = temp;
        }

        pixels = new byte[width * height * 3];
        image.getPixels().get(pixels);
    }

    /**
     * Writes Graphic Control Extension
     */
    protected void writeGraphicCtrlExt() throws IOException {
        out.write(0x21); // extension introducer
        out.write(0xf9); // GCE label
        out.write(4); // data block size
        int transp, disp;
        if (transIndex == -1) {
            transp = 0;
            disp = 0; // dispose = no action
        } else {
            transp = 1;
            disp = 2; // force clear if using transparent color
        }
        if (dispose >= 0) {
            disp = dispose & 7; // user override
        }
        disp <<= 2;

        // packed fields
        out.write(0 | // 1:3 reserved
                disp | // 4:6 disposal
                0 | // 7 user input - 0 = none
                transp); // 8 transparency flag

        writeShort(Math.round(delay/10f)); // delay x 1/100 sec
        out.write(transIndex); // transparent color index
        out.write(0); // block terminator
    }

    /**
     * Writes Image Descriptor
     */
    protected void writeImageDesc() throws IOException {
        out.write(0x2c); // image separator
        writeShort(x); // image position x,y = 0,0
        writeShort(y);
        writeShort(width); // image size
        writeShort(height);
        // packed fields
        if (firstFrame) {
            // no LCT - GCT is used for first (or only) frame
            out.write(0);
        } else {
            // specify normal LCT
            out.write(0x80 | // 1 local color table 1=yes
                    0 | // 2 interlace - 0=no
                    0 | // 3 sorted - 0=no
                    0 | // 4-5 reserved
                    palSize); // 6-8 size of color table
        }
    }

    /**
     * Writes Logical Screen Descriptor
     */
    protected void writeLSD() throws IOException {
        // logical screen size
        writeShort(width);
        writeShort(height);
        // packed fields
        out.write((0x80 | // 1 : global color table flag = 1 (gct used)
                0x70 | // 2-4 : color resolution = 7
                0x00 | // 5 : gct sort flag = 0
                palSize)); // 6-8 : gct size

        out.write(0); // background color index
        out.write(0); // pixel aspect ratio - assume 1:1
    }

    /**
     * Writes Netscape application extension to define repeat count.
     */
    protected void writeNetscapeExt() throws IOException {
        out.write(0x21); // extension introducer
        out.write(0xff); // app extension label
        out.write(11); // block size
        writeString("NETSCAPE" + "2.0"); // app id + auth code
        out.write(3); // sub-block size
        out.write(1); // loop sub-block id
        writeShort(repeat); // loop count (extra iterations, 0=repeat forever)
        out.write(0); // block terminator
    }

    /**
     * Writes color table
     */
    protected void writePalette() throws IOException {
        out.write(colorTab, 0, colorTab.length);
        int n = (3 * 256) - colorTab.length;
        for (int i = 0; i < n; i++) {
            out.write(0);
        }
    }

    /**
     * Encodes and writes pixel data
     */
    protected void writePixels() throws IOException {
        LZWEncoder encoder = new LZWEncoder(width, height, indexedPixels, colorDepth);
        encoder.encode(out);
    }

    /**
     * Write 16-bit value to output stream, LSB first
     */
    protected void writeShort(int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    /**
     * Writes string to output stream
     */
    protected void writeString(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            out.write((byte) s.charAt(i));
        }
    }
}

//	 ==============================================================================
//	 Adapted from Jef Poskanzer's Java port by way of J. M. G. Elliott.
//	 K Weiner 12/00

class LZWEncoder {

    private static final int EOF = -1;

    private final int imgW;
	private final int imgH;

    private final byte[] pixAry;

    private final int initCodeSize;

    private int remaining;

    private int curPixel;

    // GIFCOMPR.C - GIF Image compression routines
    //
    // Lempel-Ziv compression based on 'compress'. GIF modifications by
    // David Rowley (mgardi@watdcsu.waterloo.edu)

    // General DEFINEs

    static final int BITS = 12;

    static final int HSIZE = 5003; // 80% occupancy

    // GIF Image compression - modified 'compress'
    //
    // Based on: compress.c - File compression ala IEEE Computer, June 1984.
    //
    // By Authors: Spencer W. Thomas (decvax!harpo!utah-cs!utah-gr!thomas)
    // Jim McKie (decvax!mcvax!jim)
    // Steve Davies (decvax!vax135!petsd!peora!srd)
    // Ken Turkowski (decvax!decwrl!turtlevax!ken)
    // James A. Woods (decvax!ihnp4!ames!jaw)
    // Joe Orost (decvax!vax135!petsd!joe)

    int n_bits; // number of bits/code

    int maxbits = BITS; // user settable max # bits/code

    int maxcode; // maximum code, given n_bits

    int maxmaxcode = 1 << BITS; // should NEVER generate this code

    int[] htab = new int[HSIZE];

    int[] codetab = new int[HSIZE];

    int hsize = HSIZE; // for dynamic table sizing

    int free_ent = 0; // first unused entry

    // block compression parameters -- after all codes are used up,
    // and compression rate changes, start over.
    boolean clear_flg = false;

    // Algorithm: use open addressing double hashing (no chaining) on the
    // prefix code / next character combination. We do a variant of Knuth's
    // algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
    // secondary probe. Here, the modular division first probe is gives way
    // to a faster exclusive-or manipulation. Also do block compression with
    // an adaptive reset, whereby the code table is cleared when the compression
    // ratio decreases, but after the table fills. The variable-length output
    // codes are re-sized at this point, and a special CLEAR code is generated
    // for the decompressor. Late addition: construct the table according to
    // file size for noticeable speed improvement on small files. Please direct
    // questions about this implementation to ames!jaw.

    int g_init_bits;

    int ClearCode;

    int EOFCode;

    // output
    //
    // Output the given code.
    // Inputs:
    // code: A n_bits-bit integer. If == -1, then EOF. This assumes
    // that n_bits =< wordsize - 1.
    // Outputs:
    // Outputs code to the file.
    // Assumptions:
    // Chars are 8 bits long.
    // Algorithm:
    // Maintain a BITS character long buffer (so that 8 codes will
    // fit in it exactly). Use the VAX insv instruction to insert each
    // code in turn. When the buffer fills up empty it and start over.

    int cur_accum = 0;

    int cur_bits = 0;

    int[] masks = {0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F, 0x00FF, 0x01FF,
            0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF};

    // Number of characters so far in this 'packet'
    int a_count;

    // Define the storage for the packet accumulator
    byte[] accum = new byte[256];

    // ----------------------------------------------------------------------------
    LZWEncoder(int width, int height, byte[] pixels, int color_depth) {
        imgW = width;
        imgH = height;
        pixAry = pixels;
        initCodeSize = Math.max(2, color_depth);
    }

    // Add a character to the end of the current packet, and if it is 254
    // characters, flush the packet to disk.
    void char_out(byte c, OutputStream outs) throws IOException {
        accum[a_count++] = c;
        if (a_count >= 254)
            flush_char(outs);
    }

    // Clear out the hash table

    // table clear for block compress
    void cl_block(OutputStream outs) throws IOException {
        cl_hash(hsize);
        free_ent = ClearCode + 2;
        clear_flg = true;

        output(ClearCode, outs);
    }

    // reset code table
    void cl_hash(int hsize) {
        for (int i = 0; i < hsize; ++i)
            htab[i] = -1;
    }

    void compress(int init_bits, OutputStream outs) throws IOException {
        int fcode;
        int i /* = 0 */;
        int c;
        int ent;
        int disp;
        int hsize_reg;
        int hshift;

        // Set up the globals: g_init_bits - initial number of bits
        g_init_bits = init_bits;

        // Set up the necessary values
        clear_flg = false;
        n_bits = g_init_bits;
        maxcode = MAXCODE(n_bits);

        ClearCode = 1 << (init_bits - 1);
        EOFCode = ClearCode + 1;
        free_ent = ClearCode + 2;

        a_count = 0; // clear packet

        ent = nextPixel();

        hshift = 0;
        for (fcode = hsize; fcode < 65536; fcode *= 2)
            ++hshift;
        hshift = 8 - hshift; // set hash code range bound

        hsize_reg = hsize;
        cl_hash(hsize_reg); // clear hash table

        output(ClearCode, outs);

        outer_loop:
        while ((c = nextPixel()) != EOF) {
            fcode = (c << maxbits) + ent;
            i = (c << hshift) ^ ent; // xor hashing

            if (htab[i] == fcode) {
                ent = codetab[i];
                continue;
            } else if (htab[i] >= 0) // non-empty slot
            {
                disp = hsize_reg - i; // secondary hash (after G. Knott)
                if (i == 0)
                    disp = 1;
                do {
                    if ((i -= disp) < 0)
                        i += hsize_reg;

                    if (htab[i] == fcode) {
                        ent = codetab[i];
                        continue outer_loop;
                    }
                } while (htab[i] >= 0);
            }
            output(ent, outs);
            ent = c;
            if (free_ent < maxmaxcode) {
                codetab[i] = free_ent++; // code -> hashtable
                htab[i] = fcode;
            } else
                cl_block(outs);
        }
        // Put out the final code.
        output(ent, outs);
        output(EOFCode, outs);
    }

    // ----------------------------------------------------------------------------
    void encode(OutputStream os) throws IOException {
        os.write(initCodeSize); // write "initial code size" byte

        remaining = imgW * imgH; // reset navigation variables
        curPixel = 0;

        compress(initCodeSize + 1, os); // compress and write the pixel data

        os.write(0); // write block terminator
    }

    // Flush the packet to disk, and reset the accumulator
    void flush_char(OutputStream outs) throws IOException {
        if (a_count > 0) {
            outs.write(a_count);
            outs.write(accum, 0, a_count);
            a_count = 0;
        }
    }

    final int MAXCODE(int n_bits) {
        return (1 << n_bits) - 1;
    }

    // ----------------------------------------------------------------------------
    // Return the next pixel from the image
    // ----------------------------------------------------------------------------
    private int nextPixel() {
        if (remaining == 0)
            return EOF;

        --remaining;

        byte pix = pixAry[curPixel++];

        return pix & 0xff;
    }

    void output(int code, OutputStream outs) throws IOException {
        cur_accum &= masks[cur_bits];

        if (cur_bits > 0)
            cur_accum |= (code << cur_bits);
        else
            cur_accum = code;

        cur_bits += n_bits;

        while (cur_bits >= 8) {
            char_out((byte) (cur_accum & 0xff), outs);
            cur_accum >>= 8;
            cur_bits -= 8;
        }

        // If the next entry is going to be too big for the code size,
        // then increase it, if possible.
        if (free_ent > maxcode || clear_flg) {
            if (clear_flg) {
                maxcode = MAXCODE(n_bits = g_init_bits);
                clear_flg = false;
            } else {
                ++n_bits;
                if (n_bits == maxbits)
                    maxcode = maxmaxcode;
                else
                    maxcode = MAXCODE(n_bits);
            }
        }

        if (code == EOFCode) {
            // At EOF, write the rest of the buffer.
            while (cur_bits > 0) {
                char_out((byte) (cur_accum & 0xff), outs);
                cur_accum >>= 8;
                cur_bits -= 8;
            }

            flush_char(outs);
        }
    }
}
