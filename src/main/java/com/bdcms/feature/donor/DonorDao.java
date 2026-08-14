package com.bdcms.feature.donor;

import com.bdcms.shared.db.DaoException;
import com.bdcms.shared.util.Dates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * All SQL against the {@code donors} table (FR-2).
 *
 * <p>Note {@link #search} builds its WHERE clause dynamically but still binds
 * every user value as a parameter - the SQL text itself is assembled only from
 * literals this class owns, never from request input.
 */
public class DonorDao {

    private static final String COLUMNS =
            "donor_id, national_id, full_name, date_of_birth, gender, blood_group, "
            + "contact_no, address, photo_path, pin_hash, registered_on";

    public Optional<Donor> findById(Connection conn, long donorId) {
        String sql = "SELECT " + COLUMNS + " FROM donors WHERE donor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load donor " + donorId, e);
        }
    }

    public Optional<Donor> findByNationalId(Connection conn, String nationalId) {
        String sql = "SELECT " + COLUMNS + " FROM donors WHERE national_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nationalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not look up national ID", e);
        }
    }

    /**
     * Free-text search over name, national ID and contact number, optionally
     * narrowed by blood group.
     */
    public List<Donor> search(Connection conn, String query, BloodGroup bloodGroup, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM donors WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append(" AND (full_name LIKE ? OR national_id LIKE ? OR contact_no LIKE ?)");
            String like = "%" + query.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (bloodGroup != null) {
            sql.append(" AND blood_group = ?");
            params.add(bloodGroup.name());
        }
        sql.append(" ORDER BY registered_on DESC, donor_id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<Donor> donors = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    donors.add(map(rs));
                }
            }
            return donors;
        } catch (SQLException e) {
            throw new DaoException("Donor search failed", e);
        }
    }

    /** Row count matching the same filters as {@link #search}, for pagination. */
    public int countSearch(Connection conn, String query, BloodGroup bloodGroup) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM donors WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append(" AND (full_name LIKE ? OR national_id LIKE ? OR contact_no LIKE ?)");
            String like = "%" + query.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (bloodGroup != null) {
            sql.append(" AND blood_group = ?");
            params.add(bloodGroup.name());
        }

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Donor count failed", e);
        }
    }

    public long insert(Connection conn, Donor donor) {
        String sql = "INSERT INTO donors (national_id, full_name, date_of_birth, gender, "
                + "blood_group, contact_no, address, photo_path, pin_hash) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, donor.getNationalId());
            ps.setString(2, donor.getFullName());
            ps.setDate(3, Dates.toSqlDate(donor.getDateOfBirth()));
            ps.setString(4, donor.getGender().name());
            ps.setString(5, donor.getBloodGroup().name());
            ps.setString(6, donor.getContactNo());
            ps.setString(7, donor.getAddress());
            ps.setString(8, donor.getPhotoPath());
            ps.setString(9, donor.getPinHash());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                donor.setDonorId(id);
                return id;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not register donor", e);
        }
    }

    public void update(Connection conn, Donor donor) {
        String sql = "UPDATE donors SET full_name = ?, date_of_birth = ?, gender = ?, "
                + "blood_group = ?, contact_no = ?, address = ? WHERE donor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donor.getFullName());
            ps.setDate(2, Dates.toSqlDate(donor.getDateOfBirth()));
            ps.setString(3, donor.getGender().name());
            ps.setString(4, donor.getBloodGroup().name());
            ps.setString(5, donor.getContactNo());
            ps.setString(6, donor.getAddress());
            ps.setLong(7, donor.getDonorId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not update donor " + donor.getDonorId(), e);
        }
    }

    public void updatePhotoPath(Connection conn, long donorId, String photoPath) {
        String sql = "UPDATE donors SET photo_path = ? WHERE donor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, photoPath);
            ps.setLong(2, donorId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not update photo for donor " + donorId, e);
        }
    }

    public void updatePinHash(Connection conn, long donorId, String pinHash) {
        String sql = "UPDATE donors SET pin_hash = ? WHERE donor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pinHash);
            ps.setLong(2, donorId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not update PIN for donor " + donorId, e);
        }
    }

    /**
     * Donor headcount per blood group, largest first.
     *
     * <p>Feeds the dashboard rail. Groups with no donors are omitted rather
     * than shown as zero rows - an empty bar tells staff nothing.
     */
    public Map<BloodGroup, Integer> countByBloodGroup(Connection conn) {
        String sql = "SELECT blood_group, COUNT(*) AS n FROM donors "
                + "GROUP BY blood_group ORDER BY n DESC";
        Map<BloodGroup, Integer> counts = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Optional<BloodGroup> group = BloodGroup.parse(rs.getString("blood_group"));
                if (group.isPresent()) {
                    counts.put(group.get(), rs.getInt("n"));
                }
            }
            return counts;
        } catch (SQLException e) {
            throw new DaoException("Could not count donors by blood group", e);
        }
    }

    public int countAll(Connection conn) {
        String sql = "SELECT COUNT(*) FROM donors";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DaoException("Could not count donors", e);
        }
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private Donor map(ResultSet rs) throws SQLException {
        Donor donor = new Donor();
        donor.setDonorId(rs.getLong("donor_id"));
        donor.setNationalId(rs.getString("national_id"));
        donor.setFullName(rs.getString("full_name"));
        donor.setDateOfBirth(Dates.date(rs, "date_of_birth"));
        donor.setGender(Gender.parse(rs.getString("gender")).orElse(Gender.OTHER));
        donor.setBloodGroup(BloodGroup.parse(rs.getString("blood_group")).orElse(null));
        donor.setContactNo(rs.getString("contact_no"));
        donor.setAddress(rs.getString("address"));
        donor.setPhotoPath(rs.getString("photo_path"));
        donor.setPinHash(rs.getString("pin_hash"));
        donor.setRegisteredOn(Dates.dateTime(rs, "registered_on"));
        return donor;
    }
}
