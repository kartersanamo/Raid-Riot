package com.kartersanamo.raidriot.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Centers chat lines using default Minecraft font metrics (154px chat width).
 */
public final class CenteredChat {

    private static final int CHAT_WIDTH_PX = 154;
    private static final int SPACE_PX = 4;

    private CenteredChat() {
    }

    public static String center(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        String colored = ChatColor.translateAlternateColorCodes('&', message);
        int messagePx = visibleWidth(colored);
        int halved = messagePx / 2;
        int toCompensate = CHAT_WIDTH_PX - halved;
        int spaceCount = Math.max(0, toCompensate / SPACE_PX);
        StringBuilder builder = new StringBuilder(spaceCount + colored.length());
        for (int i = 0; i < spaceCount; i++) {
            builder.append(' ');
        }
        return builder.append(colored).toString();
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(center(message));
    }

    public static void broadcast(Plugin plugin, String message) {
        String centered = center(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(centered);
        }
        plugin.getLogger().info(ChatColor.stripColor(centered));
    }

    private static int visibleWidth(String colored) {
        int width = 0;
        boolean colorCode = false;
        boolean bold = false;
        for (char character : colored.toCharArray()) {
            if (character == ChatColor.COLOR_CHAR) {
                colorCode = true;
                continue;
            }
            if (colorCode) {
                colorCode = false;
                bold = character == 'l' || character == 'L';
                continue;
            }
            width += FontWidth.width(character, bold);
            bold = false;
        }
        return width;
    }

    private enum FontWidth {
        A('A', 5), B('B', 5), C('C', 5), D('D', 5), E('E', 5), F('F', 5), G('G', 5), H('H', 5),
        I('I', 3), J('J', 5), K('K', 5), L('L', 5), M('M', 5), N('N', 5), O('O', 5), P('P', 5),
        Q('Q', 5), R('R', 5), S('S', 5), T('T', 5), U('U', 5), V('V', 5), W('W', 5), X('X', 5),
        Y('Y', 5), Z('Z', 5), a('a', 5), b('b', 5), c('c', 5), d('d', 5), e('e', 5), f('f', 4),
        g('g', 5), h('h', 5), i('i', 1), j('j', 5), k('k', 4), l('l', 1), m('m', 5), n('n', 5),
        o('o', 5), p('p', 5), q('q', 5), r('r', 5), s('s', 5), t('t', 4), u('u', 5), v('v', 5),
        w('w', 5), x('x', 5), y('y', 5), z('z', 5), NUM_1('1', 5), NUM_2('2', 5), NUM_3('3', 5),
        NUM_4('4', 5), NUM_5('5', 5), NUM_6('6', 5), NUM_7('7', 5), NUM_8('8', 5), NUM_9('9', 5),
        NUM_0('0', 5), EXCLAMATION('!', 1), AT('@', 5), HASH('#', 5), DOLLAR('$', 5), PERCENT('%', 5),
        CARET('^', 5), AMP('&', 5), ASTERISK('*', 5), LEFT_PAREN('(', 3), RIGHT_PAREN(')', 3),
        MINUS('-', 5), UNDERSCORE('_', 5), PLUS('+', 5), EQUALS('=', 5), LEFT_BRACE('{', 3),
        RIGHT_BRACE('}', 3), LEFT_BRACKET('[', 3), RIGHT_BRACKET(']', 3), COLON(':', 1),
        SEMICOLON(';', 1), DOUBLE_QUOTE('"', 3), SINGLE_QUOTE('\'', 1), LESS_THAN('<', 5),
        GREATER_THAN('>', 5), QUESTION('?', 5), SLASH('/', 5), BACKSLASH('\\', 5), PIPE('|', 1),
        TILDE('~', 5), BACKTICK('`', 2), COMMA(',', 1), PERIOD('.', 1), SPACE(' ', 4);

        private final char character;
        private final int length;

        FontWidth(char character, int length) {
            this.character = character;
            this.length = length;
        }

        static int width(char character, boolean bold) {
            for (FontWidth fontWidth : values()) {
                if (fontWidth.character == character) {
                    return bold ? fontWidth.length + 1 : fontWidth.length;
                }
            }
            return bold ? 5 : 4;
        }
    }
}
