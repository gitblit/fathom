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

import fathom.utils.CryptoUtil;
import org.sonatype.micromailer.imp.AbstractMailType;

import javax.inject.Singleton;

/**
 * The template mail type.
 */
@Singleton
public class TemplateMailType extends AbstractMailType {

    public TemplateMailType(String typeId, String subjectTemplate, String bodyTemplateName, boolean isHtml) {
        setTypeId(typeId);
        setBodyIsHtml(isHtml);
        setSubjectTemplate(subjectTemplate);
        setBodyTemplate(bodyTemplateName);
    }

    public static String getTypeId(String subjectTemplate, String bodyTemplateName) {
        return CryptoUtil.getHashSHA1(subjectTemplate + bodyTemplateName);
    }
}
