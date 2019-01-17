package it.eng.knowage.engine.cockpit.api.export;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import it.eng.knowage.engine.cockpit.api.AbstractCockpitEngineResource;
import it.eng.knowage.engine.cockpit.api.export.excel.ExcelExporter;
import it.eng.spagobi.commons.utilities.SpagoBIUtilities;
import it.eng.spagobi.utilities.engines.SpagoBIEngineServiceExceptionHandler;

@Path("/1.0/cockpit/export")
public class CockpitExportResource extends AbstractCockpitEngineResource {

	static private Logger logger = Logger.getLogger(CockpitExportResource.class);
	private static final String OUTPUT_TYPE = "outputType";

	@GET
	@Path("/excel")
	public void exportToExcel() {
		logger.debug("IN");
		response.setCharacterEncoding("UTF-8");
		String dispatchUrl = null;
		try {
			String outputType = request.getParameter(OUTPUT_TYPE);
			request.setAttribute("template", getIOManager().getTemplateAsString());
			// For now, only XLSX is supported
			if (outputType.equalsIgnoreCase("xlsx")) {
				dispatchUrl = "/WEB-INF/jsp/ngCockpitExportExcel.jsp";
				response.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			}

			request.getRequestDispatcher(dispatchUrl).forward(request, response);
		} catch (Exception e) {
			logger.error("Cannot redirect to jsp", e);
			throw SpagoBIEngineServiceExceptionHandler.getInstance().getWrappedException("", getEngineInstance(), e);
		} finally {
			logger.debug("OUT");
		}
	}

	@GET
	@Path("/excelstream")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public void exportToExcelStreaming() {
		// ResponseBuilder response = null;
		// HttpServletResponse response = null;
		OutputStream out = null;
		String fileName = "test.xlsx";
		try {
			String outputType = request.getParameter("outputType");
			String userId = request.getParameter("user_id");
			Integer documentId = Integer.valueOf(request.getParameter("document"));
			String template = (String) request.getAttribute("template");
			Map<String, String[]> parameterMap = request.getParameterMap();

			String resourcePath = SpagoBIUtilities.getResourcePath();
			String directoryPath = resourcePath + File.separatorChar + "export" + File.separatorChar + "cockpit" + File.separatorChar;
			File dir = new File(directoryPath);
			if (!dir.exists() && dir.mkdirs()) {
				logger.debug("Directory [export/cockpit] is created");
			} else {
				logger.debug("Cannot create directory [export/cockpit]");
			}

			ExcelExporter excelExporter = new ExcelExporter(outputType, userId, parameterMap);
			// file = excelExporter.getExcelFile(directoryPath, fileName, template, documentId);

			// response = Response.ok();

			String mimeType = excelExporter.getMimeType();
			response.setHeader("Content-Disposition", "attachment; fileName=" + fileName);
			response.setContentType("application/vnd.openxml");
			// response.header("Content-Type", mimeType);
			// response.header("Content-Disposition", "attachment; fileName=" + fileName + "; fileType=" + outputType + "; extensionFile=" + outputType);

			out = response.getOutputStream();
			excelExporter.getFakeFile(out);
			out.close();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		// return response.build();
	}

}
