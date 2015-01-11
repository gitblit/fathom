/*
 * Copyright (C) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fathom.mailer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.Service;
import fathom.conf.RequireSetting;
import fathom.conf.Settings;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.EmailerConfiguration;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.MailType;
import org.sonatype.micromailer.imp.DefaultMailType;
import org.sonatype.micromailer.imp.HtmlMailType;

import java.util.Map;
import java.util.UUID;

/**
 * Mailer creates & sends MailRequests.
 *
 * @author James Moger
 */
@RequireSetting("mail.server")
@Singleton
public class Mailer implements Service {

    @Inject
    Settings settings;
    @Inject
    EMailer eMailer;
    @Inject
    FtmMailTypes mailTypes;

    @Override
    public int getPreferredStartOrder() {
        return 50;
    }

    @Override
    public void start() {
        // configure the Sisu EMailer
        EmailerConfiguration config = new EmailerConfiguration();
        config.setMailHost(settings.getRequiredString(Setting.mail_server));
        config.setMailPort(settings.getInteger(Setting.mail_port, 25));
        config.setUsername(settings.getString(Setting.mail_username, null));
        config.setPassword(settings.getString(Setting.mail_password, null));
        config.setBounceAddress(settings.getString(Setting.mail_bounceAddress, null));
        config.setSsl(settings.getBoolean(Setting.mail_useSsl, false));
        config.setTls(settings.getBoolean(Setting.mail_useTls, false));
        config.setDebug(settings.getBoolean(Setting.mail_debug, false));

        eMailer.configure(config);
    }

    @Override
    public void stop() {
        eMailer.shutdown();
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates a plain text MailRequest with the specified subject and body.
     * A request id is automatically generated.
     *
     * @param subject
     * @param body
     * @return a text mail request
     */
    public MailRequest newTextMailRequest(String subject, String body) {
        return createMailRequest(generateRequestId(), false, subject, body);
    }

    /**
     * Creates a plan text MailRequest with the specified subject and body.
     * The request id is supplied.
     *
     * @param requestId
     * @param subject
     * @param body
     * @return a text mail request
     */
    public MailRequest newTextMailRequest(String requestId, String subject, String body) {
        return createMailRequest(requestId, false, subject, body);
    }

    /**
     * Creates an html MailRequest with the specified subject and body.
     * The request id is automatically generated.
     *
     * @param subject
     * @param body
     * @return an html mail request
     */
    public MailRequest newHtmlMailRequest(String subject, String body) {
        return createMailRequest(generateRequestId(), true, subject, body);
    }

    /**
     * Creates an html MailRequest with the specified subject and body.
     * The request id is supplied.
     *
     * @param requestId
     * @param subject
     * @param body
     * @return an html mail request
     */
    public MailRequest newHtmlMailRequest(String requestId, String subject, String body) {
        return createMailRequest(requestId, true, subject, body);
    }

    private MailRequest createMailRequest(String requestId, boolean isHtml, String subject, String body) {
        MailRequest request = new MailRequest(requestId, isHtml ? HtmlMailType.HTML_TYPE_ID : DefaultMailType.DEFAULT_TYPE_ID);
        request.setExpandedSubject(subject);
        request.setExpandedBody(body);
        return request;
    }

    /**
     * Creates a MailRequest from the specified template.
     * The request id is automatically generated.
     *
     * @param subjectTemplate  a string that uses the parameters & TemplateEngine to interpolate values
     * @param textTemplateName the name of the classpath template resource
     * @param parameters
     * @return a text mail request
     */
    public MailRequest newTextTemplateMailRequest(String subjectTemplate, String textTemplateName, Map<String, Object> parameters) {
        return createTemplateMailRequest(generateRequestId(), subjectTemplate, textTemplateName, false, parameters);
    }

    /**
     * Creates a MailRequest from the specified template.
     * The request id is supplied.
     *
     * @param subjectTemplate  a string that uses the parameters & TemplateEngine to interpolate values
     * @param textTemplateName the name of the classpath template resource
     * @param parameters
     * @return a text mail request
     */
    public MailRequest newTextTemplateMailRequest(String requestId, String subjectTemplate, String textTemplateName, Map<String, Object> parameters) {
        return createTemplateMailRequest(requestId, subjectTemplate, textTemplateName, false, parameters);
    }

    /**
     * Creates a MailRequest from the specified template.
     * The request id is automatically generated.
     *
     * @param subjectTemplate  a string that uses the parameters & TemplateEngine to interpolate values
     * @param htmlTemplateName the name of the classpath template resource
     * @param parameters
     * @return an html mail request
     */
    public MailRequest newHtmlTemplateMailRequest(String subjectTemplate, String htmlTemplateName, Map<String, Object> parameters) {
        return createTemplateMailRequest(generateRequestId(), subjectTemplate, htmlTemplateName, true, parameters);
    }

    /**
     * Creates a MailRequest from the specified template.
     * The request id is supplied.
     *
     * @param subjectTemplate  a string that uses the parameters & TemplateEngine to interpolate values
     * @param htmlTemplateName the name of the classpath template resource
     * @param parameters
     * @return an html mail request
     */
    public MailRequest newHtmlTemplateMailRequest(String requestId, String subjectTemplate, String htmlTemplateName, Map<String, Object> parameters) {
        return createTemplateMailRequest(requestId, subjectTemplate, htmlTemplateName, true, parameters);
    }

    private MailRequest createTemplateMailRequest(String requestId, String subjectTemplate, String bodyTemplateName, boolean isHtml, Map<String, Object> parameters) {
        String typeId = TemplateMailType.getTypeId(subjectTemplate, bodyTemplateName);
        if (mailTypes.getMailType(typeId) == null) {
            // register this template
            MailType mailType = new TemplateMailType(typeId, subjectTemplate, bodyTemplateName, isHtml);
            mailTypes.addMailType(mailType);
        }

        MailRequest request = new MailRequest(requestId, typeId);
        if (parameters != null) {
            request.getBodyContext().putAll(parameters);
        }
        return request;
    }

    /**
     * Sends the MailRequest asynchronously.
     *
     * @param request
     */
    public void send(MailRequest request) {
        eMailer.sendMail(request);
    }

    /**
     * Sens the MailRequest synchronously.
     *
     * @param request
     */
    public void sendSynchronously(MailRequest request) {
        eMailer.sendSyncedMail(request);
    }

    public static enum Setting {
        mail_server, mail_port, mail_username, mail_password, mail_useSsl, mail_useTls, mail_debug, mail_systemName,
        mail_systemAddress, mail_bounceAddress;

        @Override
        public String toString() {
            return name().replace('_', '.');
        }
    }

}
