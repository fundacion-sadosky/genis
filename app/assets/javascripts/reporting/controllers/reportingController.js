define([], function() {
    'use strict';

    function reportingController ($scope, cryptoService, alertService) {

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        $scope.search = {};

        $scope.datepickers = {
            hourFrom: false,
            hourUntil: false
        };

        $scope.dateOptions = {
            initDate: new Date()
        };


        $scope.generarReporte = function () {
            if ($scope.search === undefined  || $scope.search.hourFrom === undefined|| $scope.search.hourUntil === undefined) {
                alertService.error({message:'Debe completar las fechas desde y hasta'});
                return;
            }
            var fechaDesde = $scope.search.hourFrom.getDate() + "-" + $scope.search.hourFrom.getMonth() + "-" + $scope.search.hourFrom.getFullYear();
            var fechaHasta = $scope.search.hourUntil.getDate() + "-" + $scope.search.hourUntil.getMonth() + "-" + $scope.search.hourUntil.getFullYear();
            var url = cryptoService.encryptBase64("/reportes/profilesReporting/" + fechaDesde + "/" + fechaHasta);
            var a = document.createElement("a");
            document.body.appendChild(a);
            a.style = "display: none";
            a.download = "Reporte Perfiles.pdf";
            a.href = url;
            a.click();
            document.body.removeChild(a);
        };

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

    }

    return reportingController;

});