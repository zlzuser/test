<!doctype html>
<html lang="zh-CN">
<head>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <!-- Bootstrap CSS -->
    <link href="${request.contextPath}/webjars/bootstrap/4.1.0/css/bootstrap.min.css" rel="stylesheet">
    <link href="${request.contextPath}/assets/css/form-validation.css" rel="stylesheet">
    <title>陆楚楚安卓端安装包上传</title>
</head>

<body class="bg-light">

<div class="container">
    <div class="py-5 text-center">
        <h2>上传更新包</h2>
    </div>
    <div class="row">
        <div class="col-md-6 offset-md-3">
            <form class="needs-validation" novalidate action="${request.contextPath}/apk/file/upload" method="post"
                  enctype="multipart/form-data">
                <hr class="mb-4">
                <div class="mb-3">
                    <label for="apk">APK文件</label>
                    <input type="file" class="form-control-file" name="file" id="apk" accept=".apk" required>
                    <div class="invalid-feedback">
                        请浏览你要上传的APK文件.
                    </div>
                </div>
                <div class="custom-control custom-checkbox">
                    <input type="checkbox" class="custom-control-input" value="F_YES" name="is_install" id="is_install">
                    <label class="custom-control-label" for="is_install">是否强制更新</label>
                </div>
                <p class="mb-4"></p>
                <div class="mb-3">
                    <label for="updata_log">更新记录</label>
                    <textarea class="form-control" name="updata_log" id="updata_log" required></textarea>
                    <div class="invalid-feedback">
                        请输入本次更新日志.
                    </div>
                </div>
                <p class="mb-4"></p>
                <button class="btn btn-primary btn-lg btn-block" type="submit">上传</button>
            </form>
        </div>
    </div>
    <footer class="my-5 pt-5 text-muted text-center text-small">
        <p class="mb-1">&copy; 2017-2018 Company clou</p>
    </footer>

    <!-- Modal -->
    <div class="modal fade" id="msgModal" tabindex="-1" role="dialog" aria-labelledby="msgModalLabel"
         aria-hidden="true">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="msgModalLabel">提示信息</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <p id="msg"></p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">关闭</button>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Optional JavaScript -->
<!-- jQuery first, then Popper.js, then Bootstrap JS -->
<script src="${request.contextPath}/webjars/jquery/3.3.1-1/jquery.min.js"></script>
<script src="${request.contextPath}/webjars/bootstrap/4.1.0/js/bootstrap.min.js"></script>
<script src="${request.contextPath}/webjars/jquery-form/4.2.1/jquery.form.min.js"></script>
<script src="${request.contextPath}/assets/js/vendor/holder.min.js"></script>
<script src="${request.contextPath}/assets/js/vendor/popper.min.js"></script>
<script src="${request.contextPath}/assets/js/main.js"></script>
</body>
</html>