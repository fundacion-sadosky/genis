define(['qrcodejs', 'jquery'], function(QRCode, $) {
'use strict';

var ClearPassCtrl = function($scope, userService, roleService, $modal, alertService) {

	$scope.step = 'request';
	$scope.solicitude = { superuser: false };
	$scope.challenge = {};
	$scope.regexEmail = new RegExp("^[_\\w-\\+]+(\\.[_\\w-]+)*@[\\w-]+(\\.\\w+)*(\\.[A-Za-z]{2,})$");
	$scope.regexTelNum= new RegExp(/^[0-9\-\+]{7,15}$/);
	$scope.regexTotp = new RegExp(/^\d{6}$/);
	$scope.totpSecret = null;
    $scope.solicitudClear = { };

	roleService.getRoles().then(function(response) {
		$scope.roles = response.data;
	});


    $scope.showClearPasswordForm = function(){
        var signupModalInstance = $modal.open({
            templateUrl:'/assets/javascripts/signup/clearPassword.html',
            controller: 'clearPassController',
            backdrop: 'static',
            resolve: {
                data: function () {
                }
            }
        });

        signupModalInstance.result.then(function () {
        }, function () {
        });
    };

	var drawQRCode = function(text) {
		$('#qrcode').empty();
		var qrcode = new QRCode("qrcode");
		qrcode.makeCode(text);
	};
	
	var changes = -1;
	function setSecret(userName){
		var qrCodeUri = "otpauth://totp/" + userName + "@genis?secret=" + $scope.totpSecret;
		drawQRCode(qrCodeUri);
		$scope.showCodeWarning = changes++ > 0;
		$scope.disableQr = !$scope.challenge || $scope.challenge.choosenUserName === undefined;
	}

    $scope.clearPassRequest = function(){
        userService.clearPassRequest($scope.solicitudClear).then(function(response){
            $scope.challenge.clearPassRequestId = response.data.clearPasswordRequestId;
            $scope.totpSecret = response.data.totpSecret;

            $scope.step = 'challenge';
            $scope.challenge.choosenUserName = $scope.solicitudClear.userName;
            setSecret('Seleccione un usuario');
            $scope.isUserNameSelected = true;
        },
            function () {
              alertService.error({message: 'El usuario no se encuentra habilitado para blanquear el password. Consulte con un administrador',parent: 'clear-pass-content'});
            });
    };

	$scope.someSelected = function (object) {
         if(object){
         return Object.keys(object).some(function (key) {
              return object[key];
         });
         }
      };
    $scope.clearPasswordConfirmation = function(){
        if ($scope.isUserNameSelected) {
            userService.clearPassConfirmation($scope.challenge).then(
                function () {
                    $scope.step = 'confirmation';
                },
                function (e) {
                    if (e && e.status === 400) {
                        alertService.error({message: 'El código ingresado es inválido',parent: 'signup-content'});
                    } else {
                        alertService.error({message: e.data,parent: 'signup-content'});
                    }
                }
            );
        } else {
            alertService.error({message: 'Seleccione un nombre de usuario para GENis',parent: 'signup-content'});
        }
    };
	
	$scope.onChangeUserName = function(userName){
        setSecret(userName);
        $scope.isUserNameSelected = true;
	};
	
	$scope.hideAlert = function() {
		$scope.showCodeWarning = null; 
	};
	
	$scope.getSecret = function() { 
		return ($scope.challenge  && $scope.challenge.choosenUserName !== undefined) ? 
				$scope.totpSecret : 'Seleccione un usuario'; 
	};

};

return ClearPassCtrl;

});