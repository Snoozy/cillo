function collapseFirstPost() {
    $('.post-form').css('min-height', '34px');
    $('.post-form').css('height', '34px');
    $('.first-post-clear').addClass('displaynone');
    $('.picture_upload').addClass('displaynone');
    $('.post-title').addClass('displaynone');
    $('.post-board-wrapper').addClass('displaynone');
    $('.post-board-text').addClass('displaynone');
    $('.post-board-as-user-container').addClass('displaynone');
    $('.post-form').removeClass('post-form-expanded');
    $('.post-submit').hide();
}

function expandFirstPost() {
    $('.post-form').css('min-height', '70px');
    $('.post-form').css('height', 'auto');
    $('.post-form').trigger('autosize.resize');
    $('.first-post-clear').removeClass('displaynone');
    $('.picture_upload').removeClass('displaynone');
    $('.post-title').removeClass('displaynone');
    $('.post-board-wrapper').removeClass('displaynone');
    $('.post-board-text').removeClass('displaynone');
    $('.post-board-as-user-container').removeClass('displaynone');
    $('.post-form').addClass('post-form-expanded');
    $('.post-form').removeClass('error-border');
    $(".post-submit").show();
}

function UpdateQueryString(key, value, url) {
    if (!url) url = window.location.href;
    var re = new RegExp("([?&])" + key + "=.*?(&|#|$)(.*)", "gi"),
        hash;

    if (re.test(url)) {
        if (typeof value !== 'undefined' && value !== null)
            return url.replace(re, '$1' + key + "=" + value + '$2$3');
        else {
            hash = url.split('#');
            url = hash[0].replace(re, '$1$3').replace(/(&|\?)$/, '');
            if (typeof hash[1] !== 'undefined' && hash[1] !== null)
                url += '#' + hash[1];
            return url;
        }
    }
    else {
        if (typeof value !== 'undefined' && value !== null) {
            var separator = url.indexOf('?') !== -1 ? '&' : '?';
            hash = url.split('#');
            url = hash[0] + separator + key + '=' + value;
            if (typeof hash[1] !== 'undefined' && hash[1] !== null)
                url += '#' + hash[1];
            return url;
        }
        else
            return url;
    }
}

$(document).ready(function() {

    function htmlWithBreaks(text) {
        var htmls = [];
        var lines = text.split(/\n/);
        for (var i = 0; i < lines.length; i++) {
            htmls.push(
                jQuery(document.createElement('div')).text(lines[i]).html()
            );
        }
        return htmls.join("<br>");
    }

    $(document).on('click', 'a.boomerang', function(e) {
        window.location.href = UpdateQueryString("next", encodeURIComponent(window.location.href), $(this).attr('href'));
        return false;
    });

    $('.posts-wrapper').on('click', '.post-delete', function (e) {
        if (confirm('Delete post?')) {
            var post_id = $(this).closest('.post').data('item-id');
            var $this = $(this);
            $.ajax({
                url: '/a/post/' + post_id + '/delete',
                type: 'POST',
                dataType: 'json',
                success: function () {
                    $this.closest('.post').fadeOut(500, function () {
                        $this.remove();
                    });
                    return false;
                }
            });
        }
        return false;
    });

    $('.single-post').on('click', '.post-delete', function(e) {
        if (confirm('Delete post?')) {
            var post_id = $(this).closest('.post').data('item-id');
            $.ajax({
                url: '/a/post/' + post_id + '/delete',
                type: 'POST',
                dataType: 'json',
                success: function () {
                    window.location.replace('/');
                    return false;
                }
            });
        }
    });

    $(document).on('click', '.action-link-repost', function (e) {
        e.preventDefault();
        var post_id = $(this).closest('.post').data('item-id');
        var post_content = $(this).closest('.post-wrapper').clone();
        post_content.find('.post-actions').remove();
        post_content.find('.hr-no-margin').remove();
        post_content.find('.comments-container').remove();
        post_content.find('.post-avatar').addClass('repost-avatar');
        post_content.find('.post-avatar').removeClass('post-avatar');
        post_content.find('.post-avatar-wrapper').addClass("modal-repost-avatar");
        post_content.find('.margin-maker').removeClass('margin-maker');
        $('#repost-modal').find('.repost-post-container').empty();
        $('#repost-modal').find('.repost-post-container').append(post_content);
        $('.modal-dialog').attr('data-post-id', post_id);
        $('#repost-modal').modal();
        $('.repost-submit-button').click(function () {
            var post_id = $(this).closest('.modal-dialog').data('post-id');
            var comment = $(this).closest('.modal-content').find('.repost-comment-input').val();
            var board = $('#repost-board-select option:selected').text();
            $.ajax({
                url: '/a/repost',
                type: 'POST',
                dataType: 'json',
                data: {
                    'repost' : post_id,
                    'comment' : comment,
                    'board' : board
                },
                success: function(response) {
                    $('#repost-modal').modal('hide');
                    $('#repost-modal').find('.repost-post-container').empty();
                    $('#repost-board-select').val([]);
                    $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertAfter('.first-post');
                }
            });
        });

    });

    $('.posts-wrapper').on('click', '.action-link-reposted', function () {
        return false;
    });

    $(".main-row").on({
        mouseenter: function () {
            $(this).find('.post-functions').show();
            return true;
        },
        mouseleave: function () {
            $(this).find('.post-functions').hide();
            return true;
        }
    }, 'li.post');

    $('.following .board-follow-btn').click(function() {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var group_id = $(this).closest('.board-follow-btn-container').data('board-id');
        $.ajax({
            url: '/a/group/' + group_id + '/unfollow',
            type: 'POST',
            dataType: 'json',
            success: function() {
                location.reload();
            }
        });
    });

    $('.notfollowing .board-follow-btn').click(function() {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var group_id = $(this).closest('.board-follow-btn-container').data('board-id');
        $.ajax({
            url: '/a/group/' + group_id + '/follow',
            type: 'POST',
            dataType: 'json',
            success: function() {
                location.reload();
            }
        });
    });

    $('input[placeholder], textarea[placeholder]').placeholder();

    $('textarea').autosize();

    colorVotes('post');

    colorVotes('comment');

    function getCaret(el) {
        if (el.selectionStart) {
            return el.selectionStart;
        } else if (document.selection) {
            el.focus();

            var r = document.selection.createRange();
            if (r == null) {
                return 0;
            }

            var re = el.createTextRange(),
                rc = re.duplicate();
            re.moveToBookmark(r.getBookmark());
            rc.setEndPoint('EndToStart', re);

            return rc.text.length;
        }
        return 0;
    }

    $(".navbar-search").submit(function () {
        var query = encodeURIComponent($(".search-query").val());
        if (query == "") {
            return false;
        }
        window.location.href = "/search?q=" + query;
        return false;
    });


    $('.post-form').focus(function (e) {
        e.preventDefault();
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        expandFirstPost();
    });

    $('.post-board').focus(function() {
        $(this).removeClass('error-border');
    });

    $(document).on('focus', '.comment-val', function() {
        $(this).css('min-height', '84px');
        $(this).siblings('.comment-submit-btn').removeClass('displaynone');
    });

    $(document).on('blur', '.comment-val', function() {
        if (!$.trim($(this).val())) {
            $(this).css('height', '34px');
            $(this).css('min-height', '0');
            $(this).siblings('.comment-submit-btn').addClass('displaynone');
        }
    });

    $('.comment-val').autosize();

    $(document).on('click', '.action-link-comment', function() {

        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }

        var comment_form = $(this).closest('.post-actions').siblings('.comments-container').find('.comment-form');

        $(comment_form).removeClass('displaynone');

        $('.comment-val').autosize();

        $(comment_form).find('.comment-val').focus();

        return false;

    });

    $(document).on('click', '.action-link-like', function () {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var $this = $(this);
        var post_id = $this.closest('.post').data('item-id');

        if ($this.hasClass('liked')) {
            return false;
        }

        $.ajax({
            url: '/a/post/' + post_id + '/upvote',
            type: 'POST',
            dataType: 'json',
            beforeSend: function () {
                var amount = 1;
                if ($this.parent().siblings('.dislike').children('.action-link-dislike').hasClass('disliked')) {
                    $this.parent().siblings('.dislike').children('.action-link-dislike').removeClass('disliked');
                    amount = 2;
                }
                $this.parent().siblings('.post-like-count').text(parseInt($this.parent().siblings('.post-like-count').text()) + amount);

                $this.addClass('liked');
            },
            error: function () {
            }
        });
        colorVotes('post');
        return false;
    });

    $(document).on('click', '.action-link-dislike', function () {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var $this = $(this);
        var post_id = $this.closest('.post').data('item-id');

        if ($this.hasClass('disliked')) {
            return false;
        }

        $.ajax({
            url: '/a/post/' + post_id + '/downvote',
            type: 'POST',
            dataType: 'json',
            beforeSend: function () {
                var amount = 1;
                if ($this.parent().siblings('.like').children('.action-link-like').hasClass('liked')) {
                    $this.parent().siblings('.like').children('.action-link-like').removeClass('liked');
                    amount = 2;
                }
                $this.parent().siblings('.post-like-count').text(parseInt($this.parent().siblings('.post-like-count').text()) - amount);

                $this.addClass('disliked');
            },
            error: function () {
            }
        });
        colorVotes('post');
        return false;
    });

    $(document).on('click', '.c-action.like', function () {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var $this = $(this);
        var comment_id = $(this).closest('.comment').data('comment-id');

        $.ajax({
            url: '/a/comment/' + comment_id + '/upvote',
            type: 'POST',
            dataType: 'json',
            success: function () {
                var amount = 1;
                if ($this.siblings('.c-action.dislike').hasClass('disliked')) {
                    amount = 2;
                    $this.siblings('.c-action.dislike').removeClass('disliked');
                }

                $this.addClass('liked');
                $this.siblings('.like-count').text(parseInt($this.siblings('.like-count').text()) + amount);
                colorVotes('comment');
            },
            error: function () {
            }
        });
        return false;

    });

    $(document).on('click', '.c-action.dislike', function () {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var $this = $(this);
        var comment_id = $this.closest('.comment').data('comment-id');

        $.ajax({
            url: '/a/comment/' + comment_id + '/downvote',
            type: 'POST',
            dataType: 'json',
            success: function () {
                var amount = 1;
                if ($this.siblings('.c-action.like').hasClass('liked')) {
                    amount = 2;
                    $this.siblings('.c-action.like').removeClass('liked');
                }
                $this.addClass('disliked');
                $this.siblings('.like-count').text(parseInt($this.siblings('.like-count').text()) - amount);

                colorVotes('comment');
            }
        });
        return false;
    });

    $(document).on('click', '.c-action.reply', function(e) {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        e.preventDefault();
        var reply_form = $(this).closest('.comments-container').find('.comment-form').first().clone();
        reply_form.addClass('comment-reply');
        reply_form.find('.comment-submit-btn').attr('value', 'Reply');
        reply_form.attr('data-reply-id', $(this).closest('.comment').data('comment-id'));
        reply_form.find('.comment-val').attr('placeholder', 'Reply to comment...');
        var $comment_cont = $(this).closest('.comment-actions').siblings('.comment-children-container');
        if ($comment_cont.find('.comment-form').length == 0) {
            $comment_cont.prepend(reply_form);
        }
        $comment_cont.find('.comment-val').focus();
    });


    $(document).on('submit', '.comment-input', function(event) {
        event.preventDefault();
        var $this = $(this);
        var comment = $(this).find('.comment-val').val();
        var post_id = $(this).closest('.post').data('item-id');
        var $comment_form = $(this).closest('.comment-form');
        var parent = $this.closest('.comment-form').attr('data-reply-id');
        if (parent === undefined) {
            parent = 0;
        }
        if ($.trim(comment)) {
            $.ajax({
                url: '/a/post/' + post_id + '/comment',
                type: 'POST',
                dataType: 'json',
                data: {
                    'data': comment,
                    'parent': parent
                },
                success: function (response, textStatus, jqXHR) {
                    if (parent > 0) {
                        if ($this.closest('.comment-form').siblings('.comment').length > 0) {
                            $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertBefore($this.closest('.comment-form').siblings('.comment').eq(0));
                        } else {
                            $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertAfter($this.closest('.comment-form'));
                        }
                    } else {
                        $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertAfter($this.closest('.comment-form').siblings('.comment-start-marker'));
                    }
                },
                complete: function () {
                    $comment_form.find('.comment-val').val('');
                    $comment_form.find('.comment-val').blur();
                }
            });
        }

    });

    $(document).on('blur', '.comment-reply .comment-val', function() {
        if (!$.trim($(this).val())) {
            $(this).closest('.comment-reply').remove();
        }
    });

    function readURL(file, rand) {
        var reader = new FileReader();

        reader.onload = function(e) {
            $('.post-form').addClass('post-form-with-thumbnails');
            $('.thumbnail-container .previews').append($('<div class="preview" data-upload-id="' + rand + '"><button type="button" class="remove-post-image"><span class="glyphicon glyphicon-remove"></span></button><a href="' + e.target.result + '" target="_blank"><img src="' + e.target.result + '" style="height:48px;width:48px;"></a></div>'));
            $('.thumbnail-container').removeClass('displaynone');
        };

        reader.readAsDataURL(file);
    };

    $(document).on('click', '.remove-post-image', function() {
        var upload_id = $(this).closest('.preview').data('upload-id');
        delete media_form_data['media-' + upload_id];
        $(this).closest('.preview').remove();
        if ($('.previews').children().length < 1) {
            $('.thumbnail-container').addClass('displaynone');
        }
    });

    var media_form_data = {};

    function reset_form() {
        media_form_data = {};
    };

    $(document).on('change', '#picture_file_upload', function() {
        $(this).closest('.picture_upload').siblings('.thumbnail-container').find('.previews').empty();
        if ($(this).val() != '') {
            $.each($('#picture_file_upload')[0].files, function(i, file) {
                var rand = Math.random().toString(36).substring(2);
                media_form_data['media-' + rand] = file;
                readURL(file, rand);
            });
        }
        $(this).replaceWith($(this).clone());
    });

    jQuery.fn.shake = function(intShakes, intDistance, intDuration) {
        this.each(function() {
            $(this).css("position","relative");
            for (var x=1; x<=intShakes; x++) {
                $(this).animate({left:(intDistance*-1)}, (((intDuration/intShakes)/4)))
                    .animate({left:intDistance}, ((intDuration/intShakes)/2))
                    .animate({left:0}, (((intDuration/intShakes)/4)));
            }
        });
        return this;
    };

    $(document).on('click', '.chosen-single', function() {
        $(this).removeClass('error-border');
    });

    $('#repost-board-select').chosen();

    $('#post-board-select').chosen();

    $('.anon').tooltip({'title' : 'Your posts in this group are anonymous.'});

    $(".post-submit").click(function () {
        $(this).addClass('disabled');

        if ($('.post-form').val() == "") {
            $(this).removeClass('disabled');
            $('.post-submit').shake(3, 15, 250);
            $('.post-form').addClass('error-border');
            return false;
        }

        if ($('#post-board-select option:selected').text() == "") {
            $(this).removeClass('disabled');
            $('.post-submit').shake(4, 15, 250);
            $('#post-board-select').siblings('.chosen-container').find('.chosen-single').addClass('error-border');
            return false;
        }

        var post_content = $('.post-form').val();
        var post_title = $('.post-title').val();
        var post_board = $('#post-board-select option:selected').text();
        var user = $('.post-user').val();

        if ($('.thumbnail-container .previews').children().length > 0) {
            var media_ids = null;
            var formData = new FormData();
            for (var key in media_form_data) {
                formData.append(key, media_form_data[key]);
            }
            $.ajax({
                url: '/a/upload',
                type: 'POST',
                dataType: 'json',
                contentType: false,
                processData: false,
                data: formData,
                success: function(response) {
                    media_ids = response.media_ids.join("~")
                    $.ajax({
                        url: "/a/post",
                        type: "POST",
                        dataType: "json",
                        data: {
                            "data": post_content,
                            "title": post_title,
                            "board_name": post_board,
                            "media": media_ids,
                            "user": user
                        },
                        success: function (response, textStatus, jqXHR) {
                            $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertAfter('.first-post').find('a.fluidbox').fluidbox({closeTrigger: [{ selector: 'window', event: 'scroll'}],immediateOpen: false,debounceResize: true});
                            $('textarea.post-form').val('');
                            $('.post-title').val('');
                            $('.post-board').val('');
                            $('.previews').empty();
                            $('.thumbnail-container').addClass('displaynone');
                            collapseFirstPost();
                        },
                        error: function () {
                            alert("Unexpected error. Please try again later.");
                        },
                        complete: function () {

                        }
                    });
                }
            });
        } else {
            $.ajax({
                url: "/a/post",
                type: "POST",
                dataType: "json",
                data: {
                    "data": post_content,
                    "title": post_title,
                    "board_name": post_board,
                    "user": user
                },
                success: function (response, textStatus, jqXHR) {
                    $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertAfter('.first-post');
                    $('textarea.post-form').val('');
                    $('.post-title').val('');
                    collapseFirstPost();
                },
                error: function () {
                    alert("Unexpected error. Please try again later.");
                }
            });
        }
        reset_form();
        $(this).removeClass('disabled');
        return false;
    });

    $('div[data-placeholder]').on('keydown keypress input focus', function () {
        if (this.textContent || this.childNodes.length !== 0) {
            this.dataset.divPlaceholderContent = 'true';
        }
        else {
            delete(this.dataset.divPlaceholderContent);
        }
    });

    $('.active-ph').keypress(function() {
        if ($(this).val()) {
            $(this).addClass('filled');
        } else if (!$(this).val()) {
            $(this).removeClass('filled');
        }
    });

    $(document).on('click', '.action-link-share', function() {
        var url = $(this).data('share-url');
        FB.ui({
            method: 'share',
            href: url
        }, function(response){});
        return false;
    });

    $('.fluidbox').fluidbox({
        closeTrigger: [{ selector: 'window', event: 'scroll' }],
        immediateOpen: false,
        debounceResize: true
    });

    if ($('body').hasClass('context-home')) {
        $('.context-home .posts').neverending();
    } else if ($('body').hasClass('context-user')) {
        $('.context-user ol.posts').neverending({
            context: 'user',
            entity_id: $('body').data('user-id')
        });
    } else if ($('body').hasClass('context-board')){
        $('.context-board ol.posts').neverending({
            context: 'board',
            entity_id: $('body').data('board-id')
        });
    }

});