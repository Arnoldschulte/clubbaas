package nl.clubbaas.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimale JSON-schrijver en -lezer voor de savegames. Ondersteunt objecten (Map, volgorde blijft
 * behouden), lijsten, strings, getallen, booleans en null. Geen externe bibliotheken nodig.
 */
final class Json {
    private Json() {}

    // ---- Schrijven --------------------------------------------------------------------

    static String write(Object value) {
        StringBuilder sb = new StringBuilder(1 << 16);
        write(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            writeString(sb, (String) v);
        } else if (v instanceof Boolean || v instanceof Integer || v instanceof Long) {
            sb.append(v);
        } else if (v instanceof Float || v instanceof Double) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) d = 0;
            String s = Double.toString(d);
            sb.append(s.contains(".") || s.contains("E") ? s : s + ".0");
        } else if (v instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) v).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(sb, e.getKey());
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object o : (List<Object>) v) {
                if (!first) sb.append(',');
                first = false;
                write(sb, o);
            }
            sb.append(']');
        } else {
            throw new IllegalArgumentException("Kan niet naar JSON: " + v.getClass());
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    // ---- Lezen ------------------------------------------------------------------------

    static Object parse(String s) {
        Parser p = new Parser(s);
        p.skipWhitespace();
        Object v = p.value();
        p.skipWhitespace();
        if (p.pos != s.length()) throw new IllegalArgumentException("Onverwachte tekst op positie " + p.pos);
        return v;
    }

    private static final class Parser {
        final String s;
        int pos;

        Parser(String s) {
            this.s = s;
        }

        void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        Object value() {
            if (pos >= s.length()) throw new IllegalArgumentException("Onverwacht einde");
            char c = s.charAt(pos);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            if (s.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            if (s.startsWith("null", pos)) { pos += 4; return null; }
            return number();
        }

        Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // {
            skipWhitespace();
            if (s.charAt(pos) == '}') { pos++; return map; }
            while (true) {
                skipWhitespace();
                String key = string();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                map.put(key, value());
                skipWhitespace();
                if (s.charAt(pos) == ',') { pos++; continue; }
                expect('}');
                return map;
            }
        }

        List<Object> array() {
            List<Object> list = new ArrayList<>();
            pos++; // [
            skipWhitespace();
            if (s.charAt(pos) == ']') { pos++; return list; }
            while (true) {
                skipWhitespace();
                list.add(value());
                skipWhitespace();
                if (s.charAt(pos) == ',') { pos++; continue; }
                expect(']');
                return list;
            }
        }

        String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                char e = s.charAt(pos++);
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default: sb.append(e);
                }
            }
        }

        Object number() {
            int start = pos;
            boolean decimal = false;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '.' || c == 'e' || c == 'E') decimal = true;
                else if (!(c == '-' || c == '+' || (c >= '0' && c <= '9'))) break;
                pos++;
            }
            String n = s.substring(start, pos);
            if (n.isEmpty()) throw new IllegalArgumentException("Ongeldige waarde op positie " + start);
            return decimal ? (Object) Double.parseDouble(n) : (Object) Long.parseLong(n);
        }

        void expect(char c) {
            if (pos >= s.length() || s.charAt(pos) != c) {
                throw new IllegalArgumentException("Verwacht '" + c + "' op positie " + pos);
            }
            pos++;
        }
    }
}
