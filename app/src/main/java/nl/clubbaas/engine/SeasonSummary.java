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

    public String text() {
        StringBuilder sb = new StringBuilder();
        sb.append("Seizoen ").append(season).append("\n\n");
        sb.append("Eindpositie: ").append(position).append("e in de ").append(divisionName).append("\n");
        sb.append(outcome).append("\n");
        sb.append(Cup.NAME).append(": ").append(cupResult).append("\n");
        sb.append("Prijzengeld: ").append(Fmt.money(prizeMoney)).append("\n\n");
        sb.append("Kampioenen:\n");
        for (String c : champions) sb.append("• ").append(c).append("\n");
        if (cupWinner != null) sb.append("\nBekerwinnaar: ").append(cupWinner);
        return sb.toString();
    }
}
