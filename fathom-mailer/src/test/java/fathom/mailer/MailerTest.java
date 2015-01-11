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


import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import fathom.Services;
import fathom.conf.Settings;
import fathom.utils.ClassUtil;
import org.apache.onami.test.OnamiRunner;
import org.apache.onami.test.annotation.GuiceProvidedModules;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.MailRequest;
import ro.pippo.core.Application;
import ro.pippo.core.TemplateEngine;
import ro.pippo.freemarker.FreemarkerTemplateEngine;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

@RunWith(OnamiRunner.class)
public class MailerTest extends Assert {

    @Inject
    private Settings settings;
    @Inject
    private Mailer mailer;
    private GreenMail server;

    @GuiceProvidedModules
    public static Module createTestModule() {
        return new AbstractModule() {

            @Override
            protected void configure() {
                Settings settings = new Settings();
                bind(Settings.class).toInstance(settings);
                bind(Mailer.class);

                MailModule mailModule = new MailModule();
                ClassUtil.setField(mailModule, "services", new Services(settings));
                ClassUtil.setField(mailModule, "settings", settings);
                install(mailModule);

                Application application = new Application();
                TemplateEngine templateEngine = new FreemarkerTemplateEngine();
                templateEngine.init(application);

                bind(TemplateEngine.class).toInstance(templateEngine);

            }

        };
    }

    @Before
    public void setUp() throws Exception {

        String username = settings.getString(Mailer.Setting.mail_username, null);
        String password = settings.getString(Mailer.Setting.mail_password, null);
        int port = settings.getInteger(Mailer.Setting.mail_port, 0);
        String systemAddress = settings.getString(Mailer.Setting.mail_systemAddress, null);

        // start the test smtp server
        server = new GreenMail(new ServerSetup(port, null, ServerSetup.PROTOCOL_SMTP));
        server.setUser(systemAddress, username, password);
        server.start();

        // start the mailer service
        mailer.start();
    }

    @After
    public void tearDown() throws Exception {
        mailer.stop();
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testSendTextMail() throws Exception {
        // prepare a mail request
        MailRequest request = mailer.newTextMailRequest("Plain Text Message", "This is a plain text message");

        request.getToAddresses().add(new Address("user1@gitblit.com"));
        request.getToAddresses().add(new Address("user2@gitblit.com"));

        // send the mail and wait for completion
        mailer.sendSynchronously(request);

        // validate the receipt
        MimeMessage msgs[] = server.getReceivedMessages();
        assertEquals(request.getToAddresses().size(), msgs.length);
        String receivedBody = GreenMailUtil.getBody(msgs[0]);
        assertTrue(receivedBody.contains("plain text"));
    }

    @Test
    public void testSendHtmlMail() throws Exception {
        // prepare a mail request
        MailRequest request = mailer.newHtmlMailRequest("Html Test Message", "This is a <b>text/html</b> message");

        request.getToAddresses().add(new Address("user1@gitblit.com"));
        request.getToAddresses().add(new Address("user2@gitblit.com"));

        // send the mail and wait for completion
        mailer.sendSynchronously(request);

        // validate the receipt
        MimeMessage msgs[] = server.getReceivedMessages();
        assertEquals(request.getToAddresses().size(), msgs.length);
        String receivedBody = GreenMailUtil.getBody(msgs[0]);
        assertTrue(receivedBody.contains("<b>text/html</b>"));
    }

    @Test
    public void testSendTextTemplateMail() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("username", "Example User");
        parameters.put("systemUsername", "Fathom");

        // prepare a mail request
        MailRequest request = mailer.newTextTemplateMailRequest("Hi ${username}", "test_text", parameters);

        request.getToAddresses().add(new Address("user1@gitblit.com"));
        request.getToAddresses().add(new Address("user2@gitblit.com"));

        // send the mail and wait for completion
        mailer.sendSynchronously(request);

        // validate the receipt
        MimeMessage msgs[] = server.getReceivedMessages();
        assertEquals(request.getToAddresses().size(), msgs.length);
        String receivedBody = GreenMailUtil.getBody(msgs[0]);
        assertTrue(msgs[0].getSubject().equals("Hi Example User"));
        assertTrue(receivedBody.contains("Hello Example User!"));
    }

    @Test
    public void testSendHtmlTemplateMail() throws Exception {
        // prepare a mail request

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("username", "Example User");
        parameters.put("systemUsername", "Fathom");
        MailRequest request = mailer.newHtmlTemplateMailRequest("Hi ${username}", "test_html", parameters);

        request.getToAddresses().add(new Address("user1@gitblit.com"));
        request.getToAddresses().add(new Address("user2@gitblit.com"));

        // send the mail and wait for completion
        mailer.sendSynchronously(request);

        // validate the receipt
        MimeMessage msgs[] = server.getReceivedMessages();
        assertEquals(request.getToAddresses().size(), msgs.length);
        String receivedBody = GreenMailUtil.getBody(msgs[0]);
        assertTrue(msgs[0].getSubject().equals("Hi Example User"));
        assertTrue(receivedBody.contains("Hello <b>Example User</b>!"));
    }

}