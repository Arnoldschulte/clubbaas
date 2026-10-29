package nl.clubbaas.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Samenvatting na de laatste speelronde van een seizoen. */
public class SeasonSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    public String season;
    public String divisionName;
    public int position;
    public String outcome;        // Kampioen / Promotie / Degradatie / Handhaving
    public String cupResult;
    public long prizeMoney;
    public final List<String> champions = new ArrayList<>();
    public String cupWinner;
    public String boardVerdict;
    public String europeResult;           // null = niet meegedaan
    public final List<String> europeQualified = new ArrayList<>();
    public String europeWinner;

    public String text() {
        StringBuilder sb = new StringBuilder();
        sb.append("Seizoen ").append(season).append("\n\n");
        sb.append("Eindpositie: ").append(position).append("e in de ").append(divisionName).append("\n");
        sb.append(outcome).append("\n");
        sb.append(Cup.NAME).append(": ").append(cupResult).append("\n");
        if (europeResult != null) sb.append(Game.EUROPE_NAME).append(": ").append(europeResult).append("\n");
        sb.append("Prijzengeld: ").append(Fmt.money(prizeMoney)).append("\n\n");
        if (boardVerdict != null) sb.append("Bestuur: ").append(boardVerdict).append("\n\n");
        sb.append("Kampioenen:\n");
        for (String c : champions) sb.append("• ").append(c).append("\n");
        if (cupWinner != null) sb.append("\nBekerwinnaar: ").append(cupWinner);
        if (europeWinner != null) sb.append("\nWinnaar ").append(Game.EUROPE_NAME).append(": ").append(europeWinner);
        if (europeQualified != null && !europeQualified.isEmpty()) { // null in oude savegames
            sb.append("\n\nVolgend seizoen in de ").append(Game.EUROPE_NAME).append(": ")
                    .append(String.join(", ", europeQualified));
        }
        return sb.toString();
    }
}
