(function ($) {

    var mobiComKit = new MobiComKit();
    
    var default_options = {
        icons: {},
        defaults: {
            baseUrl: "http://mobi-com-alpha.appspot.com",
            launcher: "mobicomkit-launcher"
        }
    };
    
    $.fn.mobicomkit = function (options) {
        options = $.extend({}, default_options.defaults, options);
        mobiComKit.init(options);
    };
    
}(jQuery));

function MobiComKit() {
    
    this.init = function(options) {
        new Mobicomkit_Message(options);
    };

    var MCK_BASE_URL;
    var MCK_TOKEN;
    var APPLICATION_ID;
    var USER_NUMBER;
    var USER_COUNTRY_CODE;
    var USER_DEVICE_KEY;
    var AUTH_CODE;
    var MCK_USER_TIMEZONEOFFSET;
    var FILE_METAS = "";
    var ELEMENT_NODE = 1;
    var TEXT_NODE = 3;
    var TAGS_BLOCK = ['p', 'div', 'pre', 'form'];
    var CONTACT_MAP = new Array();
    var MckUtils = new MckUtils();
    var mckMessageService = new MckMessageService();
    var mckFileService = new MckFileService();
    var mckMessageLayout = new MckMessageLayout();
    var mckContactUtils = new MckContactUtils();
    var mckDateUtils = new MckDateUtils();
    var mckNotificationService = new MckNotificationService();
    var $mck_text_box;

    var Mobicomkit_Message = function (options) {
        var _this = this;
        
        mckMessageService.init(options);
        mckFileService.init();
        
        $mck_text_box = $("#mck-text-box");
        _this.options = options;
        _this.userId = options.userId;
        _this.appId = options.appId;
        _this.baseUrl = options.baseUrl;
        APPLICATION_ID = options.appId;
        MCK_BASE_URL = options.baseUrl;

        MckUtils.initializeApp(options);
    };
    
    function MckUtils() {
        var _this = this;
        var INITIALIZE_APP_URL = "/tab/initialize.page";
        
        _this.initializeApp = function initializeApp(options) {
            var data = "applicationId=" + options.appId + "&userId=" + options.userId + "&emailId=" + options.emailId;
            $.getJSON(MCK_BASE_URL + INITIALIZE_APP_URL + "?" + data, function (result, status) {
                if (result === "INVALID_APPID") {
                    alert("Oops! looks like incorrect application id.");
                } else if (typeof result.token !== undefined) {
                    MCK_TOKEN = result.token;
                    USER_NUMBER = result.contactNumber;
                    USER_COUNTRY_CODE = result.countryCode;
                    USER_DEVICE_KEY = result.deviceKeyString;
                    MCK_USER_TIMEZONEOFFSET = result.timeZoneOffset;
                    AUTH_CODE = btoa(result.emailId + ":" + result.deviceKeyString);
                    $.ajaxPrefilter(function (options, originalOptions, jqXHR) {
                        if (!options.beforeSend) {
                            options.beforeSend = function (jqXHR) {
                                jqXHR.setRequestHeader("Authorization", "Basic " + AUTH_CODE);
                                jqXHR.setRequestHeader("Application-Key", APPLICATION_ID);
                            }
                        }
                    });
                    MckInitializeChannel(MCK_TOKEN);

                } else {
                    alert("Unable to initiate app");
                }
            });
            
            $(document).on("click", ".mck-remove-file", function () {
                $("#mck-file-box .mck-file-lb").html("");
                $("#mck-file-box .mck-file-sz").html("");
                $("#mck-ms-sbmt").attr('disabled', false);
                $("#mck-file-box").removeClass('show').addClass('hide');
                $mck_text_box.removeClass('mck-text-wf');
                $mck_text_box.attr("required", "");
                $("#mck-textbox-container").removeClass('mck-textbox-container-wf');

                if (FILE_METAS !== "") {
                    mckFileService.deleteFileMeta(FILE_METAS);
                    FILE_METAS = "";
                }
            });
            
            $(document).on("click", ".fancybox", function (e) {
                var href = $(this).find('img').data('imgurl');
                $(this).fancybox({
                    openEffect: 'none',
                    closeEffect: 'none',
                    'padding': 0,
                    'href': href,
                    'type': 'image'
                });
            });
        };
        
        _this.textVal = function () {
            var lines = [];
            var line = [];

            var flush = function () {
                lines.push(line.join(''));
                line = [];
            };

            var sanitizeNode = function (node) {
                if (node.nodeType === TEXT_NODE) {
                    line.push(node.nodeValue);
                } else if (node.nodeType === ELEMENT_NODE) {
                    var tagName = node.tagName.toLowerCase();
                    var isBlock = TAGS_BLOCK.indexOf(tagName) !== -1;

                    if (isBlock && line.length) {
                        flush();
                    }
                    
                    if (tagName === 'img') {
                        var alt = node.getAttribute('alt') || '';
                        if (alt) {
                            line.push(alt);
                        }
                        return;
                    } else if (tagName === 'br') {
                        flush();
                    }

                    var children = node.childNodes;
                    for (var i = 0; i < children.length; i++) {
                        sanitizeNode(children[i]);
                    }

                    if (isBlock && line.length) {
                        flush();
                    }
                }
            };

            var children = $mck_text_box[0].childNodes;
            for (var i = 0; i < children.length; i++) {
                sanitizeNode(children[i]);
            }

            if (line.length) {
                flush();
            }

            return lines.join('\n');
        };
    }

    function MckMessageService() {
        var _this = this;
        var ADD_MESSAGE_URL = "/rest/ws/mobicomkit/v1/message/add";
        var MESSAGE_LIST_URL = "/rest/ws/mobicomkit/v1/message/list";
        var $mck_sidebox = $("#mck-sidebox");
        var $mck_msg_form = $("#mck-msg-form");
        var $mck_msg_sbmt = $("#mck-msg-sbmt");
        var $mck_msg_error = $("#mck-msg-error");
        var $mck_msg_response = $("#mck-msg-response");
        var $mck_response_text = $("#mck_response_text");
        var $mck_msg_to = $("#mck-msg-to");
        var $messageModalLink;

        _this.init = function init(options) {
            $messageModalLink = $("[name='" + options.launcher + "']");
            $messageModalLink.click(function (e) {
                
                $mck_msg_error.html("");
                $mck_msg_error.removeClass('show').addClass('hide');
                $mck_response_text.html("");
                $mck_msg_response.removeClass('show').addClass('hide');
                $mck_msg_form[0].reset();
                $("#mck-message-inner").html("");
                mckMessageService.loadMessageList($(this).data("mck-id"));
                if ($mck_sidebox.css('display') == 'none') {
                    $('.modal').modal('hide');
                    $mck_sidebox.modal();
                }
                $mck_msg_to.focus();
            });

            $mck_msg_form.submit(function (e) {
                if (!USER_DEVICE_KEY) {
                    alert("Unable to initiate app. Please reload page.");
                    return;
                }
                var message = MckUtils.textVal();

                if ($.trim(message).length == 0 && !FILE_METAS) {
                    $("#mck-textbox-container").addClass("text-req");
                    return false;
                }
                var messagePxy = {
                    'to': $mck_msg_to.val(),
                    'contactIds': $mck_msg_to.val(),
                    'deviceKeyString': USER_DEVICE_KEY,
                    'type': 5,
                    'message': message
                }
                if (FILE_METAS) {
                    messagePxy.fileMetaKeyStrings = FILE_METAS;
                }
                $mck_msg_sbmt.attr('disabled', true);
                $mck_msg_sbmt.html('Submitting...');
                $mck_msg_error.removeClass('show').addClass('hide');
                $mck_msg_error.html("");
                $mck_response_text.html("");
                $mck_msg_response.removeClass('show').addClass('hide');
                return _this.sendMessage(messagePxy);

            });
            $("#mck-msg-form input").on('click', function () {
                $(this).val("");
                $mck_msg_error.removeClass('show').addClass('hide');
                $mck_msg_response.removeClass('show').addClass('hide');
            });
            $("#mck-text-box").on('click', function () {
                $("#mck-textbox-container").removeClass('text-req');
            });
        };

        _this.sendMessage = function sendMessage(messagePxy) {
            $.ajax({
                type: "POST",
                url: MCK_BASE_URL + ADD_MESSAGE_URL,
                data: JSON.stringify(messagePxy),
                contentType: 'application/json',
                headers: {'Authorization': "Basic " + AUTH_CODE,
                    'Application-Key': APPLICATION_ID},
                success: function (data, status, xhr) {
                    $mck_msg_sbmt.attr('disabled', false);
                    $mck_msg_sbmt.html('Submit');
                    if (data === 'error') {
                        $mck_msg_error.html("Unable to process your request. Please try again");
                        $mck_msg_error.removeClass('hide').addClass('show');
                    } else {
                        mckMessageLayout.clearMessageField();
                    }

                },
                error: function (xhr, desc, err) {
                    $mck_msg_sbmt.attr('disabled', false);
                    $mck_msg_sbmt.html('Submit');
                    $mck_msg_error.html('Unable to process your request. Please try again.');
                    $mck_msg_error.removeClass('hide').addClass('show');
                }

            });
            return false;
        };

        _this.loadMessageList = function loadMessageList(userId) {
            var userIdParam = "";            
            if (typeof userId !== "undefined") {
                userIdParam = "&userId=" + userId;
                $("#mck-msg-to").val(userId);
            }
            $("#mck-message-cell .mck-message-inner").html("");
            
            $.ajax({
                url: MCK_BASE_URL + MESSAGE_LIST_URL + "?startIndex=0&pageSize=10" + userIdParam,
                type: 'get',
                success: function (data, status) {
                    if (data + '' == "null") {
                        //no data
                    } else if (typeof data.message.length == "undefined") {
                        mckMessageLayout.addMessage(data.message, false);
                    } else {
                        $.each(data.message, function (i, data) {
                            if (!(typeof data.to == "undefined")) {
                                mckMessageLayout.addMessage(data, false);
                            }
                        });
                    }
                },
                error: function (xhr, desc, err) {
                    alert('Unable to process your request. Please try refreshing the page.');
                }
            });
        };

    }

    function MckContactUtils() {
        var _this = this;
        _this.getContactId = function (contact) {
            var contactId = contact.contactId;
            return _this.formatContactId(contactId);
        };

        _this.formatContactId = function (contactId) {
            if (contactId.indexOf("+") === 0) {
                contactId = contactId.substring(1);
            }
            return contactId.replace(/\@/g, "AT").replace(/\./g, "DOT").replace(/\*/g, "STAR").replace(/\#/g, "HASH");
        };
    }

    function MckMessageLayout() {

        var FILE_PREVIEW_URL = "/rest/ws/file/shared/";
        var _this = this;
        var $mck_msg_sbmt = $("#mck-msg-sbmt");

        var markup = '<div class="row-fluid m-b"><div class="clear"><div class="col-lg-12"><div name="message" data-msgtype="${msgTypeExpr}" data-msgdelivered="${msgDeliveredExpr}" data-msgsent="${msgSentExpr}" data-msgtime="${msgCreatedAtTime}" data-msgcontent="${replyIdExpr}"  data-msgkeystring="${msgKeyExpr}" data-contact="${contactIdsExpr}" class="${msgFloatExpr} mck-msg-box ${msgKeyExpr} ${msgClassExpr}">' +
                '<div class="mck-msg-text" id="text-${replyIdExpr}"></div>' +
                '<div  id="file-${replyIdExpr}" class="mck-msg-text notranslate span12 attachment hide" data-filemetakeystring="${fileMetaKeyExpr}" data-filename="${fileNameExpr}" data-filesize="${fileSizeExpr}">{{html fileExpr}}</div>' +
                '</div></div>' +
                '<div id="msg-expr-${replyIdExpr}" class="${msgFloatExpr}-muted  mck-text-muted text-xs m-t-xs">${createdAtTimeExpr} <i class="${statusIconExpr} ${msgKeyExpr}-status status-icon"></i></div>' +
                '</div></div>';
        $.template("messageTemplate", markup);

        _this.addTooltip = function addTooltip(msgKeyString) {
            $("." + msgKeyString + " .icon-time").data('tooltip', false).tooltip({
                placement: "left",
                trigger: "hover",
                title: "pending"
            });
            $("." + msgKeyString + " .btn-trash").data('tooltip', false).tooltip({
                placement: "left",
                trigger: "hover",
                title: "delete"
            });
            $("." + msgKeyString + " .icon-ok-circle").data('tooltip', false).tooltip({
                placement: "left",
                trigger: "hover",
                title: "sent"
            });
            $("." + msgKeyString + " .btn-forward").data('tooltip', false).tooltip({
                placement: "left",
                trigger: "hover",
                title: "forward message"
            });
            $("." + msgKeyString + " .icon-delivered").data('tooltip', false).tooltip({
                placement: "left",
                trigger: "hover",
                title: "delivered"
            });
            $("." + msgKeyString + " .msgtype-outbox-cr").data('tooltip', false).tooltip({
                placement: "right",
                trigger: "hover",
                title: "sent via Carrier"
            });
            $("." + msgKeyString + " .msgtype-outbox-mck").data('tooltip', false).tooltip({
                placement: "right",
                trigger: "hover",
                title: "sent"
            });
            $("." + msgKeyString + " .msgtype-inbox-cr").data('tooltip', false).tooltip({
                placement: "right",
                trigger: "hover",
                title: "received via Carrier"
            });
            $("." + msgKeyString + " .msgtype-inbox-mck").data('tooltip', false).tooltip({
                placement: "right",
                trigger: "hover",
                title: "recieved"
            });
        };

        _this.getIcon = function getIcon(msgType) {
            if (msgType == 1 || msgType == 3) {
                return '<i class="icon-reply msgtype-outbox msgtype-outbox-cr via-cr"></i> ';
            }

            if (msgType == 5) {
                return '<i class="icon-reply msgtype-outbox msgtype-outbox-mck via-mck"></i> ';
            }

            if (msgType == 0) {
                return '<i class="icon-mail-forward msgtype-inbox msgtype-inbox-cr via-cr"></i> ';
            }

            if (msgType == 4) {
                return '<i class="icon-mail-forward msgtype-inbox msgtype-inbox-mck via-mck"></i> ';
            }

            if (msgType == 6) {
                return '<i class = "icon-phone call_incoming"></i> ';
            }

            if (msgType == 7) {
                return '<i class = "icon-phone call_outgoing"></i> '
            }

            return "";
        };
        
        _this.addMessage = function addMessage(msg, append) {
            if (msg.type == 6 || msg.type == 7) {
                return;
            }
            var individual = true;
            if ($("#mck-message-cell ." + msg.keyString).length > 0) {
                return;
            }
            var messageClass = "";
            var floatWhere = "msg-right";
            var statusIcon = "icon-time";
            var contactExpr = "show";
            if (msg.type == 0 || msg.type == 4 || msg.type == 6) {
                floatWhere = "msg-left";
            }
            statusIcon = mckMessageLayout.getStatusIconName(msg);
            var replyId = msg.keyString;
            var replyMessageParameters = "'" + msg.deviceKeyString + "'," + "'" + msg.to + "'" + ",'" + msg.contactIds + "'" + ",'" + replyId + "'";
            var contactIds = msg.contactIds;
            var toNumbers = msg.to;
            if (contactIds.lastIndexOf(",") == contactIds.length - 1) {
                contactIds = contactIds.substring(0, contactIds.length - 1);
            }

            if (toNumbers.lastIndexOf(",") == toNumbers.length - 1) {
                toNumbers = toNumbers.substring(0, toNumbers.length - 1);
            }

            var contactIdsArray = contactIds.split(",");
            var tos = toNumbers.split(",");
            var contactNames = '';
            var s = new Set();
            if (contactIdsArray.length > 0 && contactIdsArray[0]) {
                for (var i = 0; i < contactIdsArray.length; i++) {
                    var contact;
                    if (typeof contact == 'undefined') {
                        var contactId = contactIdsArray[i];
                        contact = {
                            'contactId': contactId,
                            'htmlId': mckContactUtils.formatContactId(contactId),
                            'displayName': contactId,
                            'name': contactId + " <" + contactId + ">" + " [" + "Main" + "]",
                            'value': contactId,
                            'rel': '',
                            'photoLink': '',
                            'email': '',
                            'unsaved': true,
                            'appVersion': null
                        };
                        CONTACT_MAP[contactId] = contact;
                    }

                    if (typeof contact != 'undefined') {
                        var name = contact.displayName;
                        var rel = contact.rel;
                        rel = typeof rel == 'undefined' || rel.length == 0 ? "" : ' [' + rel + ']';
                        var contactNumber = "";
                        if (individual == false) {
                            contactNumber = tos[i];
                        }
                        messageClass += " " + contact.htmlId;
                        if (individual == false) {
                            contactNumber += rel;
                            contactNames = contactNames + ' ' + name + '<br/>';
                        } else {
                            contactExpr = "hide";
                        }
                        s.add(tos[i]);
                    }
                }
            }
            var msgFeatExpr = "hide";

            var fileName = "";
            var fileSize = "";
            var frwdMsgExpr = msg.message;
            if (typeof msg.fileMetas !== "undefined") {
                if (typeof msg.fileMetas.length === "undefined") {
                    fileName = msg.fileMetas.name;
                    fileSize = msg.fileMetas.size;
                } else {
                    fileName = msg.fileMetas[0].name;
                    fileSize = msg.fileMetas[0].size;
                }
            }
            var msgList = [
                {
                    msgKeyExpr: msg.keyString,
                    msgDeliveredExpr: msg.delivered,
                    msgSentExpr: msg.sent,
                    msgCreatedAtTime: msg.createdAtTime,
                    msgTypeExpr: msg.type,
                    msgSourceExpr: msg.source,
                    statusIconExpr: statusIcon,
                    contactExpr: contactExpr,
                    contactIdsExpr: contactIds,
                    msgFloatExpr: floatWhere,
                    contactNamesExpr: contactNames,
                    replyIdExpr: replyId,
                    createdAtTimeExpr: mckDateUtils.getDate(msg.createdAtTime),
                    msgFeatExpr: msgFeatExpr,
                    replyMessageParametersExpr: replyMessageParameters,
                    msgClassExpr: messageClass,
                    msgExpr: frwdMsgExpr,
                    selfDestructTimeExpr: msg.timeToLive,
                    fileMetaKeyExpr: msg.fileMetaKeyStrings,
                    fileExpr: _this.getImagePath(msg),
                    fileNameExpr: fileName,
                    fileSizeExpr: fileSize
                }
            ];
            append ? $.tmpl("messageTemplate", msgList).appendTo("#mck-message-cell .mck-message-inner") : $.tmpl("messageTemplate", msgList).prependTo("#mck-message-cell .mck-message-inner");
            var msg_text = msg.message.replace(/\n/g, '<br/>');
            var emoji_template = emoji.replace_unified(msg_text);
            emoji_template = emoji.replace_colons(emoji_template);
            var $textMessage = $("#text-" + replyId);
            $textMessage.html(emoji_template);
            if (msg.type == 6 || msg.type == 7) {
                $textMessage.html(mckMessageLayout.getIcon(msg.type) + $textMessage.html());
                (msg.type == 6) ? $textMessage.addClass("call_incoming") : $textMessage.addClass('call_outgoing');
            }
            $textMessage.linkify({
                target: '_blank'
            });
            if (msg.fileMetaKeyStrings) {
                $("#file-" + replyId + " a").trigger('click');
                $("#file-" + replyId).removeClass('hide').addClass('show');
                if ($textMessage.html() == "") {
                    $textMessage.hide();
                }
            }
            
            var messageListBox = $('#mck-message-cell');
            messageListBox.animate({scrollTop: messageListBox.prop("scrollHeight")}, 0);

            this.addTooltip(msg.keyString);
        };
        
        _this.getImagePath = function getImagePath(msg) {
            if (msg.fileMetaKeyStrings && typeof msg.fileMetas != "undefined") {
                if (typeof msg.fileMetas.length === "undefined") {
                    if (msg.fileMetas.contentType.indexOf("image") != -1) {
                        if (msg.fileMetas.contentType.indexOf("svg") != -1) {
                            return '<a href="#" role="link" class="file-preview-link fancybox-media fancybox"><img src="' + MCK_BASE_URL + FILE_PREVIEW_URL + msg.fileMetaKeyStrings + '" area-hidden="true" data-imgurl="' + MCK_BASE_URL + FILE_PREVIEW_URL + msg.fileMetaKeyStrings + '"></img></a>';
                        } else {
                            return '<a href="#" role="link" class="file-preview-link fancybox-media fancybox"><img src="' + msg.fileMetas.thumbnailUrl + '" area-hidden="true" data-imgurl="' + MCK_BASE_URL + FILE_PREVIEW_URL + msg.fileMetaKeyStrings + '"></img></a>';
                        }

                    } else {
                        return '<a href="' + MCK_BASE_URL + FILE_PREVIEW_URL + '"' + msg.fileMetaKeyStrings + '" role="link" class="file-preview-link" target="_blank"><span class="file-detail"><div class="file-name"><i class="icon-paperclip"></i>&nbsp;' + msg.fileMetas.name + '</div>&nbsp;<div class="file-size">' + fileService.getFilePreviewSize(msg.fileMetas.size) + '</div></span></a>';
                    }
                } else {
                    if (msg.fileMetas[0].contentType.indexOf("image") != -1) {
                        if (msg.fileMetas[0].contentType.indexOf("svg") != -1) {
                            return '<a href="#" role="link" class="file-preview-link fancybox-media fancybox"><img src="' + MCK_BASE_URL + FILE_PREVIEW_URL + msg.fileMetaKeyStrings + '" area-hidden="true" data-imgurl="' + MCK_BASE_URL + FILE_PREVIEW_URL + msg.fileMetaKeyStrings + '"></img></a>';
                        } else {
                            return '<a href="#" role="link" class="file-preview-link fancybox-media fancybox"><img src="' + msg.fileMetas[0].thumbnailUrl + '" area-hidden="true" data-imgurl="' + MCK_BASE_URL + FILE_PREVIEW_URL + msg.fileMetaKeyStrings + '"></img></a>';
                        }

                    } else {
                        return '<a href="' + MCK_BASE_URL + FILE_PREVIEW_URL + msg.fileMetaKeyStrings + '" role="link" class="file-preview-link" target="_blank"><span class="file-detail"><div class="file-name"><i class="icon-paperclip"></i>&nbsp;' + msg.fileMetas[0].name + '</div>&nbsp;<div class="file-size">' + fileService.getFilePreviewSize(msg.fileMetas[0].size) + '</div></span></a>';
                    }
                }

            }
            return "";
        };

        _this.getStatusIcon = function getStatusIcon(msg) {
            return '<i class="' + this.getStatusIconName(msg) + ' pull-right ' + msg.keyString + '_status status-icon"></i>';
        };
        _this.getStatusIconName = function getStatusIconName(msg) {
            if (msg.type == 7 || msg.type == 6) {
                return "";
            }

            if (msg.delivered == "true" || msg.delivered == true) {
                return 'icon-delivered';
            }

            if (msg.type == 3 || (msg.type == 1 && msg.source == 0) || ((msg.sent == "true" || msg.sent == true) && msg.type != 0 && msg.type != 4)) {
                return 'icon-ok-circle';
            }


            if (msg.type == 5 || (msg.type == 1 && (msg.source == 1 || msg.source == 2))) {
                return 'icon-time';
            }
            return "";
        };

        _this.clearMessageField = function clearMessageField() {
            var $mck_textbox_container = $("#mck-textbox-container");
            $mck_text_box.html("");
            $mck_msg_sbmt.attr('disabled', false);
            $("#mck-file-box").removeClass('show').addClass('hide');
            $mck_text_box.removeClass('mck-text-wf');
            $mck_textbox_container.removeClass('text-req');
            $mck_textbox_container.removeClass('mck-textbox-container-wf');
            $mck_text_box.attr("required", "");            
        };

    }

    function MckFileService() {
        var _this = this;
        var FILE_UPLOAD_URL = "/rest/ws/file/url";
        var FILE_DELETE_URL = "/rest/ws/file/delete/file/meta";
        var FILE_PREVIEW_URL = "/rest/ws/file/shared/";
        var $file_upload;
        var $file_name;
        var $file_size;
        var $file_remove;
        var $file_progress;
        var $file_progressbar;
        var $textbox_container;
        var $file_box;
        var $mck_msg_sbmt;
        _this.init = function init() {
            $file_upload = $("#mck-file-up");
            $file_name = $(".mck-file-lb");
            $file_size = $(".mck-file-sz");
            $file_box = $("#mck-file-box");
            $file_progress = $("#mck-file-box .progress");
            $file_progressbar = $("#mck-file-box .progress .bar");
            $textbox_container = $("#mck-textbox-container");
            $file_remove = $("#mck-file-box .remove-file");
            $mck_msg_sbmt = $("#mck-ms-sbmt")

            $file_upload.fileupload({
                previewMaxWidth: 100,
                previewMaxHeight: 100,
                previewCrop: true,
                submit: function (e, data) {
                    var $this = $(this);
                    if (FILE_METAS !== "") {
                        mckFileService.deleteFileMeta(FILE_METAS);
                        FILE_METAS = "";
                    }
                    $mck_text_box.addClass('mck-text-wf');
                    $textbox_container.addClass('mck-textbox-container-wf');


                    $file_name.html('<a href="#">' + data.files[0].name + '</a>');
                    $file_size.html("(" + parseInt(data.files[0].size / 1024) + " KB)");
                    $file_progressbar.css('width', '0%');
                    $file_progress.removeClass('hide').addClass('show');
                    $file_remove.attr("disabled", true);
                    $file_box.removeClass('hide').addClass('show');
                    if (data.files[0].name === $("#mck-file-box .mck-file-lb a").html()) {

                        $mck_msg_sbmt.attr('disabled', true);
                        $.ajax
                                ({
                                    type: "GET",
                                    url: MCK_BASE_URL + FILE_UPLOAD_URL,
                                    data: new Date().getTime(),
                                    crosDomain: true,
                                    headers: {'Authorization': "Basic " + AUTH_CODE,
                                        'Application-Key': APPLICATION_ID},
                                    success: function (result, status, xhr) {
                                        data.url = result;
                                        $this.fileupload('send', data);

                                    },
                                    error: function (xhr, desc, err) {

                                    }
                                });
                    }
                    return false;
                },
                progressall: function (e, data) {

                    var progress = parseInt(data.loaded / data.total * 100, 10);
                    $file_progressbar.css(
                            'width',
                            progress + '%'
                            );
                },
                success: function (result, textStatus, jqXHR) {
                    var fileExpr = mckFileService.getFilePreviewPath(result, $("#mck-file-box .mck-file-lb a").html());
                    $file_remove.attr("disabled", false);
                    $file_name.html(fileExpr);
                    $file_progress.removeClass('show').addClass('hide');

                    $mck_text_box.removeAttr('required');
                    $mck_msg_sbmt.attr('disabled', false);
                    FILE_METAS = "";
                    if (typeof result.fileMeta.length === "undefined") {
                        FILE_METAS = result.fileMeta.keyString;
                    } else {
                        $.each(result.fileMeta, function (i, fileMeta) {
                            FILE_METAS += fileMeta.keyString + ",";
                        });
                    }
                    return false;
                },
                error: function (xhr, desc, err) {
                    FILE_METAS = "";
                    $(".mck-remove-file").trigger('click');
                }
            });
        };
        _this.deleteFileMeta = function deleteFileMeta(fileMetaKeyString) {
            $.ajax({
                url: MCK_BASE_URL + FILE_DELETE_URL,
                data: 'fileMetaKeyString=' + fileMetaKeyString,
                type: 'get',
                success: function (data, status) {
                },
                error: function (xhr, desc, err) {
                }
            });
        };
        _this.getFilePreviewPath = function getFilePreviewPath(fileMetaKeyString, fileName) {
            var name = (fileName) ? fileName : "file_attached";
            if (fileMetaKeyString) {
                return '<a href="' + FILE_PREVIEW_URL + fileMetaKeyString + '" target="_blank">' + name + '</a>';
            }
            return "";
        };
        _this.getFilePreviewSize = function getFilePreviewSize(fileSize) {
            if (fileSize) {
                return "(" + parseInt(fileSize / 1024) + " KB)";
            }
            return "";
        };
    }


    function MckNotificationService() {
        var _this = this;
        _this.getChannelToken = function getChannelToken() {
            $.ajax({
                url: MCK_BASE_URL + '/rest/ws/channel/getToken',
                type: 'get',
                headers: {'Authorization': "Basic " + AUTH_CODE,
                    'Application-Key': APPLICATION_ID},
                success: function (data, status) {
                    if (data == "error") {
                        alert("Unable to process your request. Please try refreshing the page.")
                    } else {
                        MckInitializeChannel(data);
                    }

                },
                error: function (xhr, desc, err) {
                }
            });
        };
    }

    function MckInitializeChannel(token) {
        channel = new goog.appengine.Channel(token);
        //Todo: Un comment the following before deploying.
        socket = channel.open();
        socket.onopen = onOpened;
        socket.onmessage = onMessage;
        socket.onerror = onError;
        socket.onclose = onClose;
    }

    onError = function () {
        mckNotificationService.getChannelToken();
    };
    onOpened = function () {
        connected = true;
    };
    onClose = function () {
        connected = false;
    };
    onMessage = function (response) {
        var data = response.data;
        var resp = JSON.parse(data);
        var messageType = resp.type;
        if (messageType.indexOf("SMS") != -1) {
            var message = JSON.parse(resp.message);
            if (messageType == "SMS_RECEIVED") {
                mckMessageLayout.addMessage(message, true);
                //Todo: use contactNumber instead of contactId for Google Contacts API.
                var contactId = message.contactIds.replace(",", "");
                if (resp.notifyUser) {
                    //notificationService.notifyUser(msg);
                }

            } else if (messageType === "SMS_SENDING") {
                mckMessageLayout.addMessage(message, true);
                if (message.type == 3) {
                    $("." + message.keyString + "_status").removeClass('icon-time').addClass('icon-ok-circle');
                    mckMessageLayout.addTooltip(message.keyString);
                }
            } else if (messageType === "SMS_SENT_UPDATE" && message.type != 0 && message.type != 4) {
                $("." + message.keyString + "_status").removeClass('icon-time').addClass('icon-ok-circle');
                mckMessageLayout.addTooltip(message.keyString);
            }
        }
    };


    function MckDateUtils() {
        var _this = this;
        var fullDateFormat = "mmm d, yyyy h:MM TT";
        var onlyDateFormat = "mmm d";
        var onlyTimeFormat = "h:MM TT"
        var mailDateFormat = "mmm d, yyyy"
        var months = new Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

        _this.getDate = function getDate(createdAtTime) {
            var date = new Date(parseInt(createdAtTime, 10));
            var localDate = new Date();
            var utcTime = parseInt(date.getTime() + (localDate.getTimezoneOffset() * 60000));
            date = new Date(parseInt(utcTime + parseInt(MCK_USER_TIMEZONEOFFSET, 10)));
            var currentDate = new Date();
            var utcCurrentTime = parseInt(currentDate.getTime() + (localDate.getTimezoneOffset() * 60000));
            currentDate = new Date(parseInt(utcCurrentTime + parseInt(MCK_USER_TIMEZONEOFFSET, 10)));
            return currentDate.getDate() != date.getDate() ? dateFormat(date, fullDateFormat, false) : dateFormat(date, onlyTimeFormat, false);
        };


        _this.getSystemDate = function getSystemDate(time) {
            var date = new Date(parseInt(time, 10));
            return dateFormat(date, fullDateFormat, false);
        }

        var dateFormat = function () {
            var token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
                    timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
                    timezoneClip = /[^-+\dA-Z]/g,
                    pad = function (val, len) {
                        val = String(val);
                        len = len || 2;
                        while (val.length < len)
                            val = "0" + val;
                        return val;
                    };
            // Regexes and supporting functions are cached through closure
            return function (date, mask, utc) {
                var dF = dateFormat;
                // You can't provide utc if you skip other args (use the "UTC:" mask prefix)
                if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
                    mask = date;
                    date = undefined;
                }

                // Passing date through Date applies Date.parse, if necessary
                date = date ? new Date(date) : new Date;
                if (isNaN(date))
                    throw SyntaxError("invalid date");
                mask = String(mask);
                // mask = String(dF.masks[mask] || mask || dF.masks["default"]);

                // Allow setting the utc argument via the mask
                if (mask.slice(0, 4) == "UTC:") {
                    mask = mask.slice(4);
                    utc = true;
                }

                var _ = utc ? "getUTC" : "get",
                        d = date[_ + "Date"](),
                        D = date[_ + "Day"](),
                        m = date[_ + "Month"](),
                        y = date[_ + "FullYear"](),
                        H = date[_ + "Hours"](),
                        M = date[_ + "Minutes"](),
                        s = date[_ + "Seconds"](),
                        L = date[_ + "Milliseconds"](),
                        o = utc ? 0 : date.getTimezoneOffset(),
                        flags = {
                            d: d,
                            dd: pad(d),
                            ddd: dF.i18n.dayNames[D],
                            dddd: dF.i18n.dayNames[D + 7],
                            m: m + 1,
                            mm: pad(m + 1),
                            mmm: dF.i18n.monthNames[m],
                            mmmm: dF.i18n.monthNames[m + 12],
                            yy: String(y).slice(2),
                            yyyy: y,
                            h: H % 12 || 12,
                            hh: pad(H % 12 || 12),
                            H: H,
                            HH: pad(H),
                            M: M,
                            MM: pad(M),
                            s: s,
                            ss: pad(s),
                            l: pad(L, 3),
                            L: pad(L > 99 ? Math.round(L / 10) : L),
                            t: H < 12 ? "a" : "p",
                            tt: H < 12 ? "am" : "pm",
                            T: H < 12 ? "A" : "P",
                            TT: H < 12 ? "AM" : "PM",
                            Z: utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
                            o: (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
                            S: ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
                        };
                return mask.replace(token, function ($0) {
                    return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
                });
            };
        }();
// Some common format strings
        dateFormat.masks = {
            "default": "mmm d, yyyy h:MM TT",
            fullDateFormat: "mmm d, yyyy h:MM TT",
            onlyDateFormat: "mmm d",
            onlyTimeFormat: "h:MM TT",
            mailDateFormat: "mmm d, yyyy",
            mediumDate: "mmm d, yyyy",
            longDate: "mmmm d, yyyy",
            fullDate: "dddd, mmmm d, yyyy",
            shortTime: "h:MM TT",
            mediumTime: "h:MM:ss TT",
            longTime: "h:MM:ss TT Z",
            isoDate: "yyyy-mm-dd",
            isoTime: "HH:MM:ss",
            isoDateTime: "yyyy-mm-dd'T'HH:MM:ss",
            isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
        };
// Internationalization strings
        dateFormat.i18n = {
            dayNames: [
                "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
            ],
            monthNames: [
                "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
                "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
            ]
        };
    }
};