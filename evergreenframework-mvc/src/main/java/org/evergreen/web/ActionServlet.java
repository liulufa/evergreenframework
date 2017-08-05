package org.evergreen.web;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.evergreen.web.exception.ActionException;
import org.evergreen.web.view.DefaultViewResult;

/**
 * 核心控制器,接受所有请求,并将请求分发给不同的业务控制器
 */
public class ActionServlet extends FrameworkServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 789342728181721564L;

	/**
	 * 核心入口
	 */
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		try {
			// 初始化ActionContext对象
			initActionContext(request, response);
			// 请求映射,找到匹配的Action描述,返回ActionMapping对象
			ActionMapper mapper = handlerMapping.handler();
			// 如果mapping没有匹配的Action描述定义来处理请求,则当前请求交给默认servlet处理
			if (mapper.getDefinition() == null) {
				forwardDefaultServlet(request, response);
			} else {
				// 执行请求处理服务，并返回试图结果集
				ViewResult viewResult = handlerInvoker.invoke(mapper);
				// 响应视图
				response(viewResult);
				// 清除ActionContext的本地线程副本
				cleanActionContext();
			}
		} catch(IOException e) {
			e.printStackTrace();
			throw e;
		} catch(Throwable e) {
			e.printStackTrace();
			rethrowError(e);
			throw new ServletException(e.getMessage());
		}
	}

	/**
	 * 初始化contextMap对象
	 *
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ActionException
	 */
	private void initActionContext(HttpServletRequest request,
						   HttpServletResponse response) {
		Map<String, Object> contextMap = ActionContext.getContext()
				.getContextMap();
		// 将request对象放入contextMap中
		contextMap.put(REQUEST, request);
		// 将response对象放入contextMap中
		contextMap.put(RESPONSE, response);
		// 构建HttpServletRequest的map代理,放入contextMap中
		contextMap.put(REQUEST_MAP,
				new ScopeMapContext(REQUEST_MAP).createScopeProxyMap());
		// 构建HttpSession的map代理,放入contextMap中
		contextMap.put(SESSION_MAP,
				new ScopeMapContext(SESSION_MAP).createScopeProxyMap());
		// 构建ServletContext的map代理,放入contextMap中
		contextMap.put(APPLICATION_MAP,
				new ScopeMapContext(APPLICATION_MAP).createScopeProxyMap());
	}

	/**
	 * 处理视图结果
	 * 
	 * @param viewResult 视图结果对象
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	private void response(ViewResult viewResult) throws Exception {
		if (viewResult != null) {
			viewResult = (viewResult instanceof ViewResult) ? viewResult
					: new DefaultViewResult(viewResult);
			viewResult.execute();
		}
	}

	/**
	 * 将异常重抛给容器
	 * @param e
	 * @throws IOException
	 */
	private void rethrowError(Throwable e) {
		HttpServletResponse response = (HttpServletResponse) ActionContext.getContext().get(FrameworkServlet.RESPONSE);
		if(e instanceof ActionException){
			try {
				response.sendError(((ActionException)e).getResponseStatus(), e.getMessage());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 清除ActionContext的本地线程副本
     */
	private void cleanActionContext(){
		ActionContext.localContext.remove();
	}

}
