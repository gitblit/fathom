/*
 * Copyright (c) 2008-2012 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package fathom.mailer;

import com.google.inject.Inject;
import org.sonatype.micromailer.MailType;
import org.sonatype.micromailer.MailTypeSource;
import org.sonatype.micromailer.imp.DefaultMailType;
import org.sonatype.micromailer.imp.HtmlMailType;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The provider of MailTypes for Fathom Mailer.
 *
 * @author James Moger
 */
@Singleton
@Named
public class FtmMailTypes implements MailTypeSource {

    private final Map<String, MailType> mailTypes;

    @Inject
    public FtmMailTypes() {
        mailTypes = new ConcurrentHashMap<>();
        addMailType(new DefaultMailType());
        addMailType(new HtmlMailType());
    }

    public synchronized void addMailType(MailType mailType) {
        mailTypes.put(mailType.getTypeId(), mailType);
    }

    @Override
    public Collection<MailType> getKnownMailTypes() {
        return mailTypes.values();
    }

    @Override
    public MailType getMailType(String id) {
        if (mailTypes.containsKey(id)) {
            return mailTypes.get(id);
        } else {
            return null;
        }
    }
}
