package com.ib.urireg.experimental;

import com.ib.indexui.navigation.NavigationData;
import com.ib.indexui.navigation.NavigationDataHolder;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Stack;

public class RequestNavFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestNavFilter.class);

	@Inject
	NavigationDataHolder holder;
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {



		String uri = ((HttpServletRequest) request).getRequestURI();
		HttpServletRequest httpRequest = (HttpServletRequest) request;

        LOGGER.debug("=============== doFilter({}):{} ================", httpRequest.getMethod(), uri);
		Map<String, String[]> parameterMap = request.getParameterMap();
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
//			String[] value = entry.getValue();

            LOGGER.debug("{}:{}", entry.getKey(), String.join(",", entry.getValue()));
	    }
//		parameterMap.forEach((k, v) -> LOGGER.debug((k + ":" + v)));


		HttpSession session = ((HttpServletRequest) request).getSession(false);
		NavigationDataHolder holder2 = (session != null) ? (NavigationDataHolder) session.getAttribute("navigationSessionDataHolder") : null;

		if (holder!=null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("===== BCPath (Filter)===");
				Stack<NavigationData> stackPages = ((Stack<NavigationData>) holder.getPageList().clone());

				for (Object page : stackPages) {
					NavigationData pageData = (NavigationData) page;
                    LOGGER.debug("===== {}", pageData.getViewId(), pageData.getIndexInStack());
				}
			}
		}else {
			LOGGER.debug("holder is null");

		}
		LOGGER.debug("=====================================");
//		if (uri.startsWith("/static/")) {
		    chain.doFilter(request, response); // Goes to default servlet.
//		} else {
//		    request.getRequestDispatcher(uri + ".xhtml").forward(request, response); // Goes to faces servlet.
//		}

	}

}
