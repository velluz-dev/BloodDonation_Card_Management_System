package com.bdcms.feature.donor;

import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.time.LocalDate;

/**
 * New donor registration (FR-2).
 *
 * <p>{@code @MultipartConfig} is what lets the photograph arrive with the rest
 * of the form. Because {@link com.bdcms.shared.web.CsrfFilter} deliberately
 * skips multipart bodies, this servlet performs the CSRF check itself once the
 * container has parsed the parts.
 *
 * <p>On success it redirects to the card feature's issue screen rather than
 * calling it directly. That keeps the donor package free of any compile-time
 * dependency on the card package - the two features are joined by a URL, not by
 * an import - while still giving staff the natural "register then issue card"
 * flow.
 */
@WebServlet(name = "donorRegisterServlet", urlPatterns = "/app/donors/register")
@MultipartConfig(fileSizeThreshold = 512 * 1024, maxFileSize = 2 * 1024 * 1024, maxRequestSize = 4 * 1024 * 1024)
public class DonorRegisterServlet extends BaseServlet {

    private final DonorService donorService = new DonorService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showForm(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!requireCsrf(request, response)) {
            return;
        }

        Donor donor = bind(request);
        String pin = param(request, "pin");
        Part photo = photoPart(request);

        DonorService.Result result;
        try {
            result = donorService.register(donor, pin, photo);
        } catch (PhotoStore.InvalidPhotoException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("donor", donor);
            showForm(request, response);
            return;
        }

        switch (result.outcome()) {
            case REGISTERED -> {
                Flash.success(request, msg(request, "flash.donorRegistered",
                        result.donor().getFullName()));
                redirect(request, response, "/app/cards/issue?donorId=" + result.donor().getDonorId());
            }
            case DUPLICATE -> {
                // FR-2: reject, and show the record that already exists.
                request.setAttribute("duplicate", result.existing());
                request.setAttribute("errorMessage",
                        "A donor with this national ID is already registered. "
                                + "The existing record is shown below.");
                request.setAttribute("donor", donor);
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                showForm(request, response);
            }
            case INVALID -> {
                request.setAttribute("fieldErrors", result.errors().asMap());
                request.setAttribute("errorMessage", "Please correct the highlighted fields.");
                request.setAttribute("donor", donor);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                showForm(request, response);
            }
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("bloodGroups", BloodGroup.values());
        request.setAttribute("genders", Gender.values());
        request.setAttribute("maxDateOfBirth",
                LocalDate.now().minusYears(com.bdcms.shared.util.Validate.MIN_DONOR_AGE));
        request.setAttribute("minDateOfBirth",
                LocalDate.now().minusYears(com.bdcms.shared.util.Validate.MAX_DONOR_AGE));
        render(request, response, "donor/register.jsp");
    }

    /** Builds a donor from the submitted form, leaving invalid values null for the validator to catch. */
    private Donor bind(HttpServletRequest request) {
        Donor donor = new Donor();
        donor.setNationalId(param(request, "nationalId"));
        donor.setFullName(param(request, "fullName"));
        donor.setDateOfBirth(dateParam(request, "dateOfBirth").orElse(null));
        donor.setGender(Gender.parse(param(request, "gender")).orElse(null));
        donor.setBloodGroup(BloodGroup.parse(param(request, "bloodGroup")).orElse(null));
        donor.setContactNo(param(request, "contactNo"));
        donor.setAddress(param(request, "address"));
        return donor;
    }

    private Part photoPart(HttpServletRequest request) throws IOException, ServletException {
        Part part = request.getPart("photo");
        return PhotoStore.isPresent(part) ? part : null;
    }
}
