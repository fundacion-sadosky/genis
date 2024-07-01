define(['lodash'], function(_) {
    'use strict';
    console.log(_);

    function ProfileExporterToLimsController($scope,profileExporterToLimsService,alertService, cryptoService) {
        $scope.search = {};

        $scope.forense = {};
        $scope.tipos = [{label: "alta", name: $.i18n.t('generics.add')}, {label: "match", name: "Match"}]; //{label: "baja", name: "Baja"},


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
            if ($scope.search.hourFrom === undefined || $scope.search.hourFrom === null ||
                $scope.search.hourUntil === undefined || $scope.search.hourUntil === null ||
                $scope.search.tipo === undefined || $scope.search.tipo === null) {

                alertService.error({message: $.i18n.t('alerts.file.mustComplete')});
                return;
            }

            profileExporterToLimsService.exporterArchivesLims($scope.search).then(function () {
                alertService.info({message: $.i18n.t('alerts.file.filesExported')});

                var file_name = "";
                var url = "";
                if($scope.search.tipo === "alta") {
                    file_name = "InputProfiles.txt";
                    url = cryptoService.encryptBase64("/get-alta-file-export");
                } else if ($scope.search.tipo === "match") {
                    file_name = "MatchesFile.txt";
                    url = cryptoService.encryptBase64("/get-match-file-export");
                } /*else if ($scope.search.tipo === "baja") {
                    file_name = "BajaPerfiles.txt";
                    url = cryptoService.encryptBase64("/get-baja-file-export");
                }*/

                var a = document.createElement("a");
                document.body.appendChild(a);
                a.style = "display: none";
                a.href = url;
                a.download = file_name;
                a.click();
                document.body.removeChild(a);
            },function (error) {
                alertService.error({message: error.data});
                console.log(error.data);
            });
        };

        $scope.checkMax = function (fieldName) {
            var aux = $scope.search[fieldName];
            var today = new Date();

            if (today - aux < 0) {
                alertService.info({message: $.i18n.t('alerts.date.before')});
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
            //var max = new Date();

            if (min === undefined || min === null) {
                min = $scope.search.hourFrom;
            }

/*            if (max - aux < 0) {

                alertService.info({message: 'La fecha debe ser anterior a la actual.'});
                $scope.search[fieldName] = undefined;

            } else {*/
                if (min - aux > 0) {
                    alertService.info({message: $.i18n.t('alerts.date.after')});
                    $scope.search[fieldName] = undefined;
                }
            //}

        };
    }

    return ProfileExporterToLimsController;

});