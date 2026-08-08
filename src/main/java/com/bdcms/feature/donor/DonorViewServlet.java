package com.bdcms.feature.donor;

import com.bdcms.shared.util.Validate;
import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.Optional;

/**
 * Donor detail and editing (FR-2).
 *
 * <p>Shows only what the donor package owns: demographics, photograph and PIN
 * status. Cards and donation history are reached by link, because they belong
 * to the card and donation features and this package does not import them.
 *
 * <p>The national ID is displayed but never editable. It is the key that makes
 * the record unique across centres; allowing it to be retyped would reintroduce
 * exactly the fragmentation the system exists to prevent.
 */
@WebServlet(name = "donorViewServlet", urlPatterns = "/app/donors/view")
@MultipartConfig(fileSizeThreshold = 512 * 1024, maxFileSize = 2 * 1024 * 1024, maxRequestSize = 4 * 1024 * 1024)
public class DonorViewServlet extends BaseServlet {

    private final DonorService donorService = new DonorService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Optional<Donor> donor = loadDonor(request, response);
        if (donor.isEmpty()) {
            return;
        }
        request.setAttribute("donor", donor.get());
        request.setAttribute("editing", request.getParameter("edit") != null);
        request.setAttribute("bloodGroups", BloodGroup.values());
        request.setAttribute("genders", Gender.values());
        render(request, response, "donor/view.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!requireCsrf(request, response)) {
            return;
        }
        Optional<Donor> existing = loadDonor(request, response);
        if (existing.isEmpty()) {
            return;
        }
        Donor donor = existing.get();

        if ("setPin".equals(param(request, "action"))) {
            updatePin(request, response, donor);
            return;
        }

        donor.setFullName(param(request, "fullName"));
        donor.setDateOfBirth(dateParam(request, "dateOfBirth").orElse(null));
        donor.setGender(Gender.parse(param(request, "gender")).orElse(null));
        donor.setBloodGroup(BloodGroup.parse(param(request, "bloodGroup")).orElse(null));
        donor.setContactNo(param(request, "contactNo"));
        donor.setAddress(param(request, "address"));

        Part photo = request.getPart("photo");
        Validate.Errors errors;
        try {
            errors = donorService.update(donor, PhotoStore.isPresent(photo) ? photo : null);
        } catch (PhotoStore.InvalidPhotoException e) {
            request.setAttribute("errorMessage", e.getMessage());
            showEdit(request, response, donor);
            return;
        }

        if (errors.hasErrors()) {
            request.setAttribute("fieldErrors", errors.asMap());
            request.setAttribute("errorMessage", "Please correct the highlighted fields.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showEdit(request, response, donor);
            return;
        }

        Flash.success(request, "Donor record updated.");
        redirect(request, response, "/app/donors/view?donorId=" + donor.getDonorId());
    }

    private void updatePin(HttpServletRequest request, HttpServletResponse response, Donor donor)
            throws IOException {
        String pin = param(request, "pin");
        boolean clearing = "clear".equals(param(request, "pinMode"));

        if (!clearing && !Validate.isPin(pin)) {
            Flash.error(request, "PIN must be exactly four digits.");
        } else {
            donorService.setPin(donor.getDonorId(), clearing ? null : pin);
            Flash.success(request, clearing
                    ? "Verification PIN removed."
                    : "Verification PIN updated.");
        }
        redirect(request, response, "/app/donors/view?donorId=" + donor.getDonorId());
    }

    private void showEdit(HttpServletRequest request, HttpServletResponse response, Donor donor)
            throws ServletException, IOException {
        request.setAttribute("donor", donor);
        request.setAttribute("editing", true);
        request.setAttribute("bloodGroups", BloodGroup.values());
        request.setAttribute("genders", Gender.values());
        render(request, response, "donor/view.jsp");
    }

    /** Loads the donor named by {@code donorId}, sending 400/404 when it cannot be found. */
    private Optional<Donor> loadDonor(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Optional<Long> donorId = longParam(request, "donorId");
        if (donorId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "donorId is required");
            return Optional.empty();
        }
        Optional<Donor> donor = donorService.findById(donorId.get());
        if (donor.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such donor");
        }
        return donor;
    }
}
