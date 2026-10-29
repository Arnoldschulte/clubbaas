package nl.clubbaas.engine;

import java.io.Serializable;

/** Een sponsorcontract. Elk seizoen zonder lopend contract kies je uit drie aanbiedingen. */
public class SponsorDeal implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        MEERJARIG("Meerjarig"),
        EENJARIG("Eenjarig"),
        PRESTATIE("Prestatiecontract");

        public final String label;

        Type(String label) {
            this.label = label;
        }
    }

    public final String sponsor;
    public final Type type;
    public final long perSeason;
    public final long winBonus;        // per competitieoverwinning
    public final long promotionBonus;  // bij promotie of kampioenschap
    public int seasonsLeft;            // inclusief het huidige seizoen

    SponsorDeal(String sponsor, Type type, long perSeason, long winBonus, long promotionBonus, int seasons) {
        this.sponsor = sponsor;
        this.type = type;
        this.perSeason = perSeason;
        this.winBonus = winBonus;
        this.promotionBonus = promotionBonus;
        this.seasonsLeft = seasons;
    }

    public String description() {
        switch (type) {
            case MEERJARIG:
                return Fmt.money(perSeason) + " per seizoen, vast voor " + seasonsLeft
                        + " seizoenen. Zekerheid, ook na degradatie, maar het groeit niet mee bij promotie.";
            case PRESTATIE:
                return Fmt.money(perSeason) + " per seizoen, plus " + Fmt.money(winBonus)
                        + " per overwinning en " + Fmt.money(promotionBonus)
                        + " bij promotie of kampioenschap. Riskant, maar lucratief als het goed gaat.";
            default:
                return Fmt.money(perSeason) + " voor één seizoen. Volgend seizoen een nieuw aanbod.";
        }
    }
}
