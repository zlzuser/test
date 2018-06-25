(function () {
    'use strict';

    window.addEventListener('load', function () {
        // Fetch all the forms we want to apply custom Bootstrap validation styles to
        var forms = document.getElementsByClassName('needs-validation');

        // Loop over them and prevent submission
        var validation = Array.prototype.filter.call(forms, function (form) {
            form.addEventListener('submit', function (event) {
                event.preventDefault();
                event.preventDefault()
                form.classList.add('was-validated');
                if (form.checkValidity() === false) {
                    return false;
                }
                $(this).ajaxSubmit({
                    dataType: "json",
                    success: function (res) {
                        $("#msg").text(res.msg);
                        $('#msgModal').modal('show');
                        setTimeout(function () {
                            $('#msgModal').modal('hide');
                        }, 3000);
                    }, error: function (res) {
                        $("#msg").text(res.msg);
                        $('#msgModal').modal('show');
                        setTimeout(function () {
                            $('#msgModal').modal('hide');
                        }, 3000);
                    }
                });
            }, false);
        });
    }, false);
})();