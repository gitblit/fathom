<#import "base.ftl" as base/>
<@base.page>
<div class="row">
    <div class="large-9 columns">
        <form method="POST">
            <div class="row">
                <div class="large-12 columns">
                    <h2>JSON Content
                        <small>PUT application/json from a form</small>
                    </h2>
                    <textarea id="desserts" name="_content">${myDesserts}</textarea>

                    <!-- dress-up our textarea wth CodeMirror -->
                    <link rel="stylesheet" href="${webjarsAt('codemirror/5.3/lib/codemirror.css')}">
                    <script src="${webjarsAt('codemirror/5.3/lib/codemirror.js')}"></script>
                    <script src="${webjarsAt('codemirror/5.3/mode/javascript/javascript.js')}"></script>
                    <style>
                        .CodeMirror { height: 200px; border: 1px solid #ddd; margin-bottom: 20px;}
                        .CodeMirror-scroll { max-height: 200px; }
                        .CodeMirror pre { padding-left: 7px; line-height: 1.25; }
                    </style>
                    <script>
                        var cm = CodeMirror.fromTextArea(desserts, {
                            lineNumbers: true,
                            tabSize: 2,
                            lineWrapping: true,
                        });
                    </script>
                </div>
            </div>
            <div class="row">
                <div class="large-12 columns">
                    <input type="hidden" name="_csrf_token" value="${csrfToken}">
                    <input type="hidden" name="_method" value="PUT">
                    <input type="hidden" name="_content_type" value="application/json">
                    <button type="submit" class="small radius">${i18n('fathom.save')}</button>
                    <a class="button small radius secondary" href="${appPath}/">${i18n('fathom.cancel')}</a>
                </div>
            </div>
        </form>
    </div>
</div>
</@base.page>