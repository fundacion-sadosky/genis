define([], function() {
    'use strict';

    function CourtCaseCloseModalController($scope, $modalInstance, info) {
        $scope.shouldDeleteAssociatedProfiles = false;
        $scope.pedigreeValidation = info.pedigreeValidation;
        $scope.profileValidation = info.profileValidation;

        $scope.isPedigreeValidation = function () {
          return $scope.pedigreeValidation;
        };

        $scope.isProfileValidation = function () {
            return $scope.profileValidation;
        };

        $scope.close = function(value) {
            var returnVal = {};
            returnVal.shouldDeleteAssociatedProfiles = $scope.shouldDeleteAssociatedProfiles;
            returnVal.goOn = value;
            $modalInstance.close(returnVal);
        };

    }

    return CourtCaseCloseModalController;

});