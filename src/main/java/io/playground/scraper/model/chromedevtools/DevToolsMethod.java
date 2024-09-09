package io.playground.scraper.model.chromedevtools;

public enum DevToolsMethod {

    DOM_DESCRIBE_NODE("DOM.describeNode"),
    DOM_DISABLE("DOM.disable"),
    DOM_ENABLE("DOM.enable"),
    DOM_FOCUS("DOM.focus"),
    DOM_GET_ATTRIBUTES("DOM.getAttributes"),
    DOM_GET_BOX_MODEL("DOM.getBoxModel"),
    DOM_GET_DOCUMENT("DOM.getDocument"),
    DOM_GET_NODE_FOR_LOCATION("DOM.getNodeForLocation"),
    DOM_GET_OUTER_HTML("DOM.getOuterHTML"),
    DOM_SET_OUTER_HTML("DOM.setOuterHTML"),
    DOM_HIDE_HIGHLIGHT("DOM.hideHighlight"),
    DOM_HIGHLIGHT_NODE("DOM.highlightNode"),
    DOM_HIGHLIGHT_REC("DOM.highlightRect"),
    DOM_MOVE_TO("DOM.moveTo"),
    DOM_QUERY_SELECTOR("DOM.querySelector"),
    DOM_QUERY_SELECTOR_ALL("DOM.querySelectorAll"),
    DOM_REMOVE_ATTRIBUTE("DOM.removeAttribute"),
    DOM_REMOVE_NODE("DOM.removeNode"),
    DOM_RESOLVE_NODE("DOM.resolveNode"),
    DOM_REQUEST_NODE("DOM.requestNode"),
    DOM_SCROLL_INTO_VIEW_IF_NEEDED("DOM.scrollIntoViewIfNeeded"),
    DOM_SET_ATTRIBUTES_AS_TEXT("DOM.setAttributesAsText"),
    DOM_SET_ATTRIBUTE_VALUE("DOM.setAttributeValue"),
    DOM_SET_FILE_INPUT_FILES("DOM.setFileInputFiles"),
    DOM_COLLECT_CLASS_NAME_FROM_SUB_TREE("DOM.collectClassNamesFromSubtree"),
    DOM_GET_ANCHOR_ELEMENT("DOM.getAnchorElement"),
    DOM_GET_CONTAINER_FOR_NODE("DOM.getContainerForNode"),
    DOM_CHILD_NODE_COUNT_UPDATED("DOM.childNodeCountUpdated"),
    DOM_PERFORM_SEARCH("DOM.performSearch"),
    
    CCS_GET_COMPUTED_STYLE_FOR_NODE("CSS.getComputedStyleForNode"),
    
    NETWORK_ENABLE("Network.enable"),
    NETWORK_AUTH_CHALLENGE_RESPONSE("Network.AuthChallengeResponse"),
    NETWORK_SET_COOKIE("Network.setCookie"),
    NETWORK_SET_COOKIES("Network.setCookies"),
    NETWORK_GET_COOKIES("Network.getCookies"),
    NETWORK_DELETE_COOKIES("Network.deleteCookies"),
    NETWORK_CLEAR_BROWSER_COOKIES("Network.clearBrowserCookies"),
    NETWORK_SET_BYPASS_SERVICE_WOKER("Network.setBypassServiceWorker"),

    STORAGE_SET_COOKIES("Storage.setCookies"),
    
    EMULATION_SET_TIME_ZONE_OVERRIDE("Emulation.setTimezoneOverride"),

    FETCH_GET_RESPONSE_BODY("Fetch.getResponseBody"),
    FETCH_CONTINUE_RESPONSE("Fetch.continueResponse"),
    FETCH_CONTINUE_REQUEST("Fetch.continueRequest"),
    FETCH_FAIL_REQUEST("Fetch.failRequest"),
    FETCH_FULFILL_REQUEST("Fetch.fulfillRequest"),
    FETCH_CONTINUE_WITH_AUTH("Fetch.continueWithAuth"),
    FETCH_ENABLE("Fetch.enable"),
    FETCH_DISABLE("Fetch.disable"),
    FETCH_AUTH_REQUIRED("Fetch.authRequired"),

    OVERLAY_ENABLE("Overlay.enable"),
    OVERLAY_DISABLE("Overlay.disable"),
    OVERLAY_HIGHLIGHT_NODE("Overlay.highlightNode"),

    
    INPUT_DISPATCH_MOUSE_EVENT("Input.dispatchMouseEvent"),
    INPUT_DISPATCH_KEY_EVENT("Input.dispatchKeyEvent"),
    INPUT_INSERT_TEXT("Input.insertText"),
    INPUT_SYNTHESIZE_SCROLL_GESTURE("Input.synthesizeScrollGesture"),

    PAGE_ENABLE("Page.enable"),
    PAGE_RELOAD("Page.reload"),
    PAGE_GET_NAVIGATION_HISTORY("Page.getNavigationHistory"),
    PAGE_NAVIGATE_TO_HISTORY_ENTRY("Page.navigateToHistoryEntry"),
    PAGE_NAVIGATE("Page.navigate"),
    PAGE_HANDLE_JAVASCRIPT_DIALOG("Page.handleJavaScriptDialog"),
    PAGE_JAVASCRIPT_DIALOG_OPENING("Page.javascriptDialogOpening"),
    PAGE_GET_FRAME_TREE("Page.getFrameTree"),
    PAGE_CREATE_ISOLATED_WORLD("Page.createIsolatedWorld"),
    PAGE_LOAD_EVENT_FIRED("Page.loadEventFired"),
    PAGE_FRAME_STOPPED_LOADING("Page.frameStoppedLoading"),
    PAGE_CAPTURE_SCREENSHOT("Page.captureScreenshot"),

    RUNTIME_EVALUATE("Runtime.evaluate"),
    RUNTIME_CALL_FUNCTION_ON("Runtime.callFunctionOn"),
    RUNTIME_RUN_IF_WAITING_FOR_DEBUGGER("Runtime.runIfWaitingForDebugger"),

    BROWSER_GET_VERSION("Browser.getVersion"),
    BROWSER_GET_WINDOW_FOR_TARGET("Browser.getWindowForTarget"),
    BROWSER_GET_WINDOW_BOUNDS("Browser.getWindowBounds"),
    BROWSER_SET_WINDOW_BOUNDS("Browser.setWindowBounds"),
    BROWSER_CLOSE("Browser.close"),
    BROWSER_SET_DOWNLOAD_BEHAVIOR("Browser.setDownloadBehavior"),

    TARGET_CREATE_BROWSER_CONTEXT("Target.createBrowserContext"),
    TARGET_DISPOSE_BROWSER_CONTEXT("Target.disposeBrowserContext"),
    TARGET_CREATE_TARGET("Target.createTarget"),
    TARGET_SET_DISCOVER_TARGETS("Target.setDiscoverTargets"),
    TARGET_SET_AUTO_ATTACH("Target.setAutoAttach"),
    TARGET_ATTACH_TO_TARGET("Target.attachToTarget"),
    TARGET_ACTIVATE_TARGET("Target.activateTarget"),
    TARGET_GET_TARGET_INFO("Target.getTargetInfo"),
    TARGET_GET_TARGETS("Target.getTargets"),
    TARGET_CLOSE_TARGET("Target.closeTarget"),
    ;

    private final String method;

    DevToolsMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }
}
