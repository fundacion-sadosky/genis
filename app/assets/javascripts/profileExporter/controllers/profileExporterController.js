define(['lodash'], function(_) {
    'use strict';
    console.log(_);

    function ProfileExporterController($scope,profileExporterService,alertService, cryptoService, userService) {
        $scope.search = {};

        $scope.categories = {};
        $scope.forense = {};
        $scope.datepickers = {
            hourFrom: false,
            hourUntil: false
        };

        $scope.dateOptions = {
            initDate: new Date()
        };
        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        $scope.exportProfiles = function () {
            profileExporterService.exporterProfiles($scope.search).then(function () {
                alertService.info({message: 'Se exportaron los perfiles'});

                var zip_file_name = "GENisExport.zip";
                var user = userService.getUser();

                var url = cryptoService.encryptBase64("/get-profile-export/" + user.name);
                var a = document.createElement("a");
                document.body.appendChild(a);
                a.style = "display: none";
                a.href = url;
                a.download = zip_file_name;
                a.click();
                document.body.removeChild(a);
            },function (error) {
                alertService.error({message: error.data});
                console.log(error.data);
            });
        };

        profileExporterService.getCategories().then(function(response) {

            $scope.categories.data = response.data;

        }, function() {
            $scope.categories = {};
            $scope.categories.data = {};
        });
        $scope.checkMax = function (fieldName) {
            var aux = $scope.search[fieldName];
            var today = new Date();

            if (today - aux < 0) {
                alertService.info({message: 'La fecha debe ser anterior a la actual.'});
                $scope.search[fieldName] = undefined;
                $scope.minDateCoin = null;
            } else {
                if (fieldName === 'hourFrom') {
                    if (aux !== undefined || aux !== null) {
                        $scope.minDateCoin = aux;
                    }
                    else {
                        $scope.minDateCoin = null;
                    }
                }
            }

        };
        $scope.toggleDatePicker = function ($event, witch) {
            $event.preventDefault();
            $event.stopPropagation();

            $scope.datepickers[witch] = !$scope.datepickers[witch];
        };
        $scope.checkMaxMin = function (fieldName, fechaMin) {
            var aux = $scope.search[fieldName];
            var min = $scope.search[fechaMin];
            var max = new Date();

            if (min === undefined || min === null) {
                min = $scope.search.hourFrom;
            }
            if (max - aux < 0) {
                alertService.info({message: 'La fecha debe ser anterior a la actual.'});
                $scope.search[fieldName] = undefined;
            } else {
                if (min - aux > 0) {
                    alertService.info({message: 'La fecha  debe ser posterior al campo desde.'});
                    $scope.search[fieldName] = undefined;
                }
            }

        };
        profileExporterService.getLaboratories().then(function (response) {
            $scope.laboratories = response.data;
        });
    }

    return ProfileExporterController;

});