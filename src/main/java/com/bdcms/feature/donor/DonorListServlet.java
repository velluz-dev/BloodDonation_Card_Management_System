package com.bdcms.feature.donor;

import com.bdcms.shared.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Donor search and browse (FR-2: "search, view and management of donor
 * information").
 */
@WebServlet(name = "donorListServlet", urlPatterns = "/app/donors")
public class DonorListServlet extends BaseServlet {

    private static final int PAGE_SIZE = 20;

    private final DonorService donorService = new DonorService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String query = param(request, "q");
        BloodGroup bloodGroup = BloodGroup.parse(param(request, "bloodGroup")).orElse(null);
        int page = Math.max(1, intParam(request, "page", 1));
        int offset = (page - 1) * PAGE_SIZE;

        List<Donor> donors = donorService.search(query, bloodGroup, PAGE_SIZE, offset);
        int total = donorService.countSearch(query, bloodGroup);
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));

        request.setAttribute("donors", donors);
        request.setAttribute("query", query);
        request.setAttribute("bloodGroup", bloodGroup);
        request.setAttribute("bloodGroups", BloodGroup.values());
        request.setAttribute("page", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("total", total);

        render(request, response, "donor/list.jsp");
    }
}
