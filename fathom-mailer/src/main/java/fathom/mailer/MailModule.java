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

import fathom.Module;
import org.kohsuke.MetaInfServices;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.MailComposer;
import org.sonatype.micromailer.MailSender;
import org.sonatype.micromailer.MailStorage;
import org.sonatype.micromailer.MailTypeSource;
import org.sonatype.micromailer.imp.DefaultEMailer;
import org.sonatype.micromailer.imp.DefaultMailSender;
import org.sonatype.micromailer.imp.DefaultMailStorage;

/**
 * @author James Moger
 */
@MetaInfServices
public class MailModule extends Module {

    @Override
    protected void setup() {
        bind(EMailer.class).to(DefaultEMailer.class);
        bind(MailSender.class).to(DefaultMailSender.class);
        bind(MailStorage.class).to(DefaultMailStorage.class);

        bind(MailComposer.class).to(FtmMailComposer.class);
        bind(MailTypeSource.class).to(FtmMailTypes.class);
        bind(Mailer.class);
    }
}
