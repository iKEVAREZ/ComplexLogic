package me.matrix89.complexlogic.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiSimpleScrolledSelectionListProxy;
import net.minecraft.item.EnumDyeColor;
import org.lwjgl.input.Keyboard;

import java.util.Random;
import java.util.regex.Pattern;

import static net.minecraft.client.gui.GuiScreen.isCtrlKeyDown;
import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;
import static net.minecraft.client.gui.GuiScreen.setClipboardString;

public class HexEditorComponent extends Gui {
    private int cursor = 0;

    private enum Nibble {UPPER, LOWER}

    private Nibble cursorNibble = Nibble.UPPER;

    public int groupSize = 2;
    public int groupsPerLine = 4;
    //spacing size in character widths
    private int spacing = 2;
    private int charWidth;
    private int scroll = 0;

    private static final int SELECTION_NONE = Integer.MAX_VALUE;
    private int selectionAnchor = SELECTION_NONE;
    private FontRenderer fontRenderer;

    private Random rnd = new Random();

    private byte[] data = new byte[256];

    private Minecraft mc;
    private int posX;
    private int posY;

    public HexEditorComponent(FontRenderer fontRenderer, int x, int y) {
        this.fontRenderer = fontRenderer;
        charWidth = fontRenderer.getCharWidth('_');
        this.posX = x;
        this.posY = y;
    }

    public void setPos(int x, int y) {
        this.posX = x;
        this.posY = y;
    }

    public void keyTyped(char typedChar, int keyCode) {
        Pattern p = Pattern.compile("[a-fA-F0-9]");
        if (!isCtrlKeyDown() && p.matcher(String.valueOf(typedChar).toUpperCase()).find()) {
            switch (cursorNibble) {
                case UPPER:
                    data[cursor] &= (data[cursor] << 8 | 0x0f);
                    data[cursor] |= Byte.parseByte("" + typedChar, 16) << 4;

                    cursorNibble = Nibble.LOWER;
                    return;
                case LOWER:
                    data[cursor] &= 0xf0;
                    data[cursor] |= Byte.parseByte("" + typedChar, 16);
                    setCursor(cursor + 1);
                    cursorNibble = Nibble.UPPER;
            }
            return;
        }

        handledMovement(keyCode);
    }

    private void mouseUpdateCursor(int mouseX, int mouseY) {
        mouseX -= posX;
        mouseY -= posY;
        int line = (mouseY / fontRenderer.FONT_HEIGHT) + scroll - 1;
        int printedSpacingWidth = mouseX / (2 * charWidth * groupSize + spacing * charWidth) + 1;
        int column = (mouseX / charWidth) / 2 - printedSpacingWidth;

        setCursor(column + line * (groupSize * groupsPerLine));
    }


    public void draw() {
        int x = posX;
        int y = posY;
        for (int i = scroll * groupSize * groupsPerLine; i < data.length; i++) {
            if (i % (groupSize * groupsPerLine) == 0) {
                y += fontRenderer.FONT_HEIGHT;
                x = posX;
            }
            if (x != 0 && i % groupSize == 0) {
                x += spacing * charWidth;
            }

            if (selectionAnchor != SELECTION_NONE && selectionAnchor != cursor && ((i >= selectionAnchor && i <= cursor) || (i <= selectionAnchor && i >= cursor))) {
                drawRect(x, y + fontRenderer.FONT_HEIGHT, x + 2 * charWidth, y, 0xff000000 | EnumDyeColor.GREEN.getColorValue());
            }
            if (i == cursor) {
                int nx = x + (cursorNibble == Nibble.UPPER ? 0 : charWidth);
                drawRect(nx, (int) (y + fontRenderer.FONT_HEIGHT * 0.9f), nx + charWidth, y + fontRenderer.FONT_HEIGHT, 0xff000000 | EnumDyeColor.BLUE.getColorValue());
            }
            fontRenderer.drawString(String.format("%02X", data[i]), x, y, EnumDyeColor.WHITE.getColorValue());
            x += 2 * charWidth;
        }

    }

    public void setCursor(int cursor) {
        this.cursor = Math.max(0, Math.min(cursor, data.length - 1));
    }

    private void handledMovement(int keyCode) {
        if (isShiftKeyDown()) {
            if (selectionAnchor == SELECTION_NONE) {
                selectionAnchor = cursor;
            }
        }
        switch (keyCode) {
            case 205:
                setCursor(++cursor);
                break;
            case 203:
                setCursor(--cursor);
                break;
            case 200://up
                setCursor(cursor - (groupSize * groupsPerLine));
                break;
            case 208://down
                setCursor(cursor + (groupSize * groupsPerLine));
                break;
            case 46: //copy
                StringBuilder sb = new StringBuilder();
                int start = Math.min(selectionAnchor, cursor);
                int end = Math.max(selectionAnchor, cursor);
                for (int i = start; i <= end; i++) {
                    sb.append(String.format("%02X", data[i]));
                }
                setClipboardString(sb.toString());
                break;
            case 47: //paste
                break;
            case 13: // +
                scroll++;
                break;
            case 12: //-
                scroll = Math.max(scroll - 1, 0);
                break;

        }
        cursorNibble = Nibble.UPPER;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (selectionAnchor == cursor) {
            selectionAnchor = SELECTION_NONE;
        }
    }

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        mouseUpdateCursor(mouseX, mouseY);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        mouseUpdateCursor(mouseX, mouseY);
        selectionAnchor = cursor;
    }
}