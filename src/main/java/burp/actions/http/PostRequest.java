package burp.actions.http;

import burp.*;

import java.net.URL;

/**
 * POST Request abstraction to simplify the generation of customized POST responses with custom request headers.
 *
 * @author thomas.hartmann@netcentric.biz
 * @since 02/2019
 */
public class PostRequest implements HttpMethod {

    private final IHttpRequestResponse baseMessage;

    private final IExtensionHelpers helpers;

    private final BurpHelperDto burpHelperDto;

    private final byte[] baseRequest;

    private byte[] postMessage;

    /**
     * {@link java.lang.reflect.Constructor}
     *
     * @param burpHelperDto
     * @param baseMessage
     */
    public PostRequest(final BurpHelperDto burpHelperDto, final IHttpRequestResponse baseMessage) {
        this.burpHelperDto = burpHelperDto;
        this.helpers = burpHelperDto.getHelpers();
        this.baseMessage = baseMessage;
        this.baseRequest = baseMessage.getRequest();
        this.helpers.toggleRequestMethod(baseRequest);
    }

    public void init(final URL url, final String... headers) {
        if (this.postMessage != null) {
            return;
        }

        final byte[] request = this.helpers.buildHttpRequest(url);
        final byte[] postRequest = this.helpers.toggleRequestMethod(request);

        this.postMessage = postRequest;
    }

    @Override
    public void addRequestParameter(final String name, final String value) {
        if (this.postMessage == null) {
            return;
        }

        final IParameter param = this.helpers.buildParameter(name, value, IParameter.PARAM_URL);
        this.postMessage = this.helpers.addParameter(this.postMessage, param);
    }

    @Override
    public void addBodyParameter(final String name, final String value) {
        if (this.postMessage == null) {
            return;
        }

        final IParameter bodyParam = this.helpers.buildParameter(name, value, IParameter.PARAM_BODY);
        this.postMessage = this.helpers.addParameter(this.postMessage, bodyParam);
    }

    public ResponseHolder send() {
        if (this.postMessage == null) {
            return ResponseHolder.createIncomplete();
        }

        final IHttpRequestResponse requestResponse = this.burpHelperDto.getCallbacks()
                .makeHttpRequest(baseMessage.getHttpService(), this.postMessage);
        this.burpHelperDto.getCallbacks().printOutput("\n Send request: \n" + this.helpers.bytesToString(requestResponse.getRequest()) + "\n" );
        return ResponseHolder.create(requestResponse);
    }

    @Override
    public ResponseHolder send(URL url) {
        this.init(url);
        return this.send();
    }

    public static PostRequest createInstance(final BurpHelperDto burpHelperDto, final IHttpRequestResponse baseMessage) {
        final PostRequest postRequest = new PostRequest(burpHelperDto, baseMessage);
        return postRequest;
    }
}
