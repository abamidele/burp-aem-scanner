package burp.util;

import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IParameter;
import burp.IRequestInfo;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper to simplify the ampunt of boilerplate for fully fledged Http requests
 *
 * @author thomas.hartmann@netcentric.biz
 * @since 02/2019
 */
public class BurpHttpRequest {

    public static final String PARAM_BODY = "body";

    private final IExtensionHelpers helpers;

    private final IHttpRequestResponse baseMessage;

    private URL url;

    private String method;

    private List<String> headers = new ArrayList<>();

    private Map<String, String> parameters = new LinkedHashMap<>();

    private String body;

    private byte[] currentRequest;

    public BurpHttpRequest(final IExtensionHelpers helpers, final IHttpRequestResponse baseMessage, final URL url) {
        this.helpers = helpers;
        this.baseMessage = baseMessage;
        this.url = url;
    }

    public Optional<byte[]> create() {
        byte[] baseRequest = null;

        if (headers.size() == 0 || StringUtils.equalsAnyIgnoreCase(getMethod(), "GET")) {
            // create initial request
            baseRequest = this.helpers.buildHttpRequest(url);

            // we have a body so add a body and make it a POST
            if (StringUtils.isNotEmpty(body)) {
                baseRequest = addRequestBody(baseRequest);
            }
        } else {
            baseRequest = postMessageFromBaseMessage(url, this.headers);
        }
        baseRequest = addUrlParameters(baseRequest);

        return Optional.ofNullable(baseRequest);
    }

    private byte[] addRequestBody(byte[] baseRequest) {
        final String bodyEncoded = this.helpers.urlEncode(body);

        final IParameter bodyParam = this.helpers.buildParameter(PARAM_BODY, bodyEncoded, IParameter.PARAM_BODY);
        baseRequest = this.helpers.addParameter(baseRequest, bodyParam);

        this.helpers.toggleRequestMethod(baseRequest);

        return baseRequest;
    }

    private byte[] postMessageFromBaseMessage(final URL url, final List<String> newHeaders) {
        byte[] baseRequest = baseMessage.getRequest();
        IRequestInfo requestInfo = this.helpers.analyzeRequest(baseMessage.getHttpService(), baseRequest);

        // ignore old headers which are set explicitly by newHeaders. keep all others e.g. for authentication
        final String[] ignores = createHeaderFilterList(newHeaders);

        final List<String> existingHeaders = requestInfo.getHeaders();
        final List<String> headers = existingHeaders
                .stream()
                .map(header -> {
                    if (StringUtils.containsAny(header, "POST", "GET")) {
                        return createPostHeader(url, header);
                    }
                    return header;
                })
                .filter(header -> StringUtils.startsWithAny(header, ignores))
                .collect(Collectors.toList());

        headers.addAll(newHeaders);

        final String bodyEncoded = this.helpers.urlEncode(body);
        return this.helpers.buildHttpMessage(headers, this.helpers.stringToBytes(body));
    }

    private byte[] addUrlParameters(byte[] baseRequest) {
        for (Map.Entry<String, String> pair : this.parameters.entrySet()) {
            final IParameter parameter = this.helpers.buildParameter(pair.getKey(), pair.getValue(), IParameter.PARAM_URL);
            baseRequest = this.helpers.addParameter(baseRequest, parameter);
        }
        return baseRequest;
    }

    private String[] createHeaderFilterList(List<String> newHeaders) {
        final List<String> ignoreOldOnes = newHeaders
                .stream()
                .map(header -> StringUtils.split(header, ":")[0])
                .collect(Collectors.toList());
        return ignoreOldOnes.toArray(new String[ignoreOldOnes.size()]);
    }

    private String createPostHeader(URL url, String header) {
        int end = header.lastIndexOf("HTTP");
        final String path = url.getPath();
        return "POST " + path + " " + header.substring(end);
    }

    private byte[] createBody(final byte[] baseRequest, final IRequestInfo requestInfo) {
        int bodyLength = baseRequest.length - requestInfo.getBodyOffset();
        byte[] body = new byte[bodyLength];
        System.arraycopy(baseRequest, requestInfo.getBodyOffset(), body, 0, bodyLength);
        return body;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public void addHeader(final String header) {
        this.headers.add(header);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void addParameter(final String key, final String value) {
        this.parameters.put(key, value);
    }
}
