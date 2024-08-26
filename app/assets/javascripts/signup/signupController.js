define(['qrcodejs', 'jquery'], function(QRCode, $) {
'use strict';

var SignupCtrl = function($scope, userService, roleService, $modal, alertService) {

	$scope.step = 'request';
	$scope.solicitude = { superuser: false };
	$scope.challenge = {};
	$scope.regexEmail = new RegExp("^[_\\w-\\+]+(\\.[_\\w-]+)*@[\\w-]+(\\.\\w+)*(\\.[A-Za-z]{2,})$");
	$scope.regexTelNum= new RegExp(/^[0-9\-\+]{7,15}$/);
	$scope.regexTotp = new RegExp(/^\d{6}$/);
	$scope.totpSecret = null;
    $scope.disclaimer = false;
    $scope.disclaimerText = "";

    userService.getDisclaimerHtml().then(function (response) {
        $scope.disclaimerText = response.data.text;
    },function(error){
		console.log("empty disclaimer "+error);
	});

	roleService.getRolesForSignUp().then(function(response) {
		$scope.roles = response.data;
	});

    $scope.showDisclaimer = function(){
        $scope.step = 'disclaimer';
    };
    $scope.goBackSignUp = function(){
        $scope.step = 'request';
    };
	$scope.showSignupForm = function(){
		var signupModalInstance = $modal.open({
			templateUrl:'/assets/javascripts/signup/signup.html',
			controller: 'signupController',
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
	
	$scope.signupRequest = function(){
		userService.signupRequest($scope.solicitude).then(function(response){
			$scope.challenge.signupRequestId = response.data.signupRequestId;
			$scope.userNameCandidates = response.data.userNameCandidates;
			$scope.totpSecret = response.data.totpSecret;
			
			$scope.step = 'challenge';
			
			setSecret($.i18n.t('generics.userSelect'));
            $scope.isUserNameSelected = false;
		});	
	};
	
	$scope.someSelected = function (object) {
         if(object){
         return Object.keys(object).some(function (key) {
              return object[key];
         });
         }
      };
	
	$scope.signupConfirmation = function(){
		if ($scope.isUserNameSelected) {
            userService.signupConfirmation($scope.challenge).then(
                function () {
                    $scope.step = 'confirmation';
                },
                function (e) {
                    if (e && e.status === 400) {
                        alertService.error({message:  $.i18n.t('error.invalidCode'),parent: 'signup-content'});
                    } else {
                        alertService.error({message: e.data,parent: 'signup-content'});
                    }
                }
            );
        } else {
            alertService.error({message:  $.i18n.t('signup.challenge.chooseUsername'),parent: 'signup-content'});
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
				$scope.totpSecret : $.i18n.t('generics.userSelect');
	};

};

return SignupCtrl;

});