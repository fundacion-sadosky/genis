define(['angular'], function(angular) {
'use strict';

function CourtCasesController ($scope, pedigreeService, $location, $filter, $modal, alertService) {

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

    $scope.noResult = true;

    $scope.status = [{label:""}].concat(["Open","Closed","Deleted"]
        .map(function(s) { return {label: $filter('i18next')('pedigree.status.' + s), value: s}; }));

    pedigreeService.getCaseTypes().then(function (response) {
        $scope.caseTypes = [{name:""}].concat(response.data);
    });

    var createSearchObject = function(filters) {
        $scope.previousFilters = angular.copy(filters);

        var searchObject = {};

        if (filters.code && filters.code.length > 0) {
            searchObject.code = filters.code;
        }

        if (filters.profile && filters.profile.length > 0) {
            searchObject.profile = filters.profile;
        }

        searchObject.status = filters.status;

        if ($scope.sortField) {
            searchObject.sortField = $scope.sortField;
        }

        if ($scope.ascending) {
            searchObject.ascending = $scope.ascending;
        }
        if(filters.caseType && filters.caseType.length>0){
            searchObject.caseType = filters.caseType;
        }else{
            searchObject.caseType = undefined;
        }

        searchObject.page = $scope.currentPage - 1;
        searchObject.pageSize = $scope.pageSize;

        return searchObject;
    };

    var searchCourtCases = function(filters) {
        $scope.isProcessing = true;
        var searchObject = createSearchObject(filters);

        pedigreeService.getTotalCourtCases(searchObject).then(function(response){
            $scope.totalItems = response.headers('X-PEDIGREES-LENGTH');
            if ($scope.totalItems === '0') {
                $scope.noResult = true;
                $scope.isProcessing = false;
            } else {
                $scope.noResult = false;
                pedigreeService.getCourtCases(searchObject).then(function(response) {
                    $scope.courtCases = response.data;
                    $scope.isProcessing = false;
                });
            }
        });

    };

	$scope.changePage = function() {
        searchCourtCases($scope.previousFilters);
	};
    
    $scope.search = function() {
        searchCourtCases($scope.filters);
    };

    var initialize = function() {
        $scope.pageSize = 30;
        $scope.currentPage = 1;

        $scope.filters = {};
        $scope.filters.status = "Open";

        $scope.search();
    };

    initialize();

    $scope.cleanFilters = function() {
        initialize();
    };

    $scope.goToCourtCase = function(cc) {
        if (cc) {
            $location.url('/court-case/' + cc.id);
        } else {
            $location.url('/court-case');
        }
    };
    
    $scope.goToGenogram = function(cc) {
      $location.url('/pedigree/' + cc.id);  
    };
    $scope.goToCaseDetail = function(cc) {
        $location.url('/court-case/' + cc.id);
    };
    $scope.deleteCourtCase = function(cc) {
        pedigreeService.canDeleteCourtCase(cc.id).then(function(response) {
            // returns if the court case can be deleted
            var editable = response.data;

            if (!editable) {
                alertService.error({message: $.i18n.t('alerts.case.deletedError')});
            } else {

                $modal.open({
                    templateUrl:'/assets/javascripts/pedigree/views/courtCase-modal-close.html',
                    controller: 'courtCaseCloseModalController',
                    resolve: {
                        info: function () {
                            return {
                                pedigreeValidation: true,
                                profileValidation: false
                            };
                        }
                    }
                }).result.then(function (value) {
                    if (value.goOn) {
                        pedigreeService.changeStatus(cc.id, 'Deleted', value.shouldDeleteAssociatedProfiles, {}).then(function () {
                            alertService.success({message: $.i18n.t('alerts.genericDelete.success')});
                            cc.status = 'Deleted';
                        }, function (response) {
                            alertService.error({message: response.data});
                        });
                    }
                });


            }
        });
    };

    $scope.isDeleted = function(cc) {
        return cc.status === 'Deleted';
    };

    $scope.isClosed = function(cc) {
        return cc.status === 'Closed';
    };

    $scope.sortBy = function(sortField) {
        $scope.currentPageLog = 1;
        if (sortField !== $scope.sortField) {
            $scope.ascending = true;
        } else {
            $scope.ascending = !$scope.ascending;
        }
        $scope.sortField = sortField;

        $scope.search();

    };

    $scope.closeCourCase = function(cc) {
        pedigreeService.canCloseCourtCase(cc.id).then(function(response) {
            // returns if the pedigree can be returned to construction mode
            var closeable = response.data;

            if (closeable) {
                $modal.open({
                    templateUrl:'/assets/javascripts/pedigree/views/courtCase-modal-close.html',
                    controller: 'courtCaseCloseModalController',
                    resolve: {
                        info: function () {
                            return {
                                pedigreeValidation: true,
                                profileValidation: false
                            };
                        }
                    }
                }).result.then(function (value) {
                    if (value.goOn) {
                        pedigreeService.changeStatus(cc.id, 'Closed', value.shouldDeleteAssociatedProfiles, {}).then(function () {
                        alertService.success({message: $.i18n.t('alerts.case.closeSuccess')});
                        cc.status = 'Closed';
                        }, function (response) {
                            alertService.error({message: response.data});
                        });
                    }
                });
            } else {
                alertService.error({message: $.i18n.t('alerts.case.pendingMatch')});
            }
        });


    };


}

return CourtCasesController;

});