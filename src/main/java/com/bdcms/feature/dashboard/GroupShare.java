package com.bdcms.feature.dashboard;

/**
 * One row of the dashboard's blood-group bar chart.
 *
 * <p>Deliberately a class with JavaBean getters rather than a record: Tomcat
 * 10.1 implements Jakarta EL 5.0, which cannot read record components as
 * properties. See {@code com.bdcms.shared.web.SessionUser} for the full note.
 *
 * <p>{@code share} is a percentage of the <em>largest</em> group, not of the
 * total. The bars are there to compare groups against each other at a glance,
 * and scaling to the biggest one uses the full width of the rail.
 */
public class GroupShare {

    private final String label;
    private final int count;
    private final int share;

    public GroupShare(String label, int count, int share) {
        this.label = label;
        this.count = count;
        this.share = share;
    }

    public String getLabel() {
        return label;
    }

    public int getCount() {
        return count;
    }

    public int getShare() {
        return share;
    }
}
