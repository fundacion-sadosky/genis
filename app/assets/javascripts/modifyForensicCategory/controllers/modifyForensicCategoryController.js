define(
  ['angular', 'jquery','lodash'],
  function(angular, $, _) {
    'use strict';
    function modifyForensicCategoryController(
      $scope ,
      $rootScope,
      $routeParams,
      $log,
      alertService,
      searchService,
      categoriesService,
      profileDataService
    ) {
      var buildSearchObject = function(profileId) {
        return {
          input: profileId,
          active: true,
          inactive: false,
          page: 0,
          pageSize: 2,
          category: "",
          notUploaded: null
        };
      };

      var getProfileData = function(profileId) {
        return profileDataService
          .getProfileData(profileId);
      };
      // Find forensic categories.
      // filter found
      $scope.models = {
        matchingCodesModel: "",
        newCategory: undefined,
        selectedProfiledata: {}
      };
      $scope.matchingCodes = [];
      $scope.categories = [];
      $scope.stage = 1;
      $scope.confirmedCode = undefined;

      $scope.picturePlaceHolderImage = 'assets/images/default-user.png';
      $scope.inprintPrintPlaceHolderImage = 'assets/images/fingerprint.jpg';
      $scope.signaturePlaceHolderImage = 'assets/images/signature.jpg';
      $scope.pictures = [];
      $scope.inprints = [];
      $scope.signatures = [];
      $scope.token = {};

      $scope.searchProfile = function() {
        if (!$scope.search) {
          alertService.warning({message: "Por favor ingrese un código."});
          return;
        }
        var errorMessage = function() {
          alertService
            .error({"message": "Perfil no encontrado."});
        };
        searchService
          .search(buildSearchObject($scope.search))
          .then(
            function(response) {
              $scope.matchingCodes = response.data;
              if ($scope.matchingCodes.length > 0) {
                $scope.stage = 2;
              }
            },
            errorMessage
          );
      };
      $scope.clearMatchingCodes = function() {
        $scope.matchingCodes = [];
        $scope.stage = 1;
      };
      $scope.confirmSelectedCode = function() {
        // TODO: Check that entered value is noy empty
        $scope.confirmedCode = $scope.models.matchingCodesModel;
        if ($scope.confirmedCode !== undefined) {
          getProfileData($scope.confirmedCode.globalCode)
            .then(
               function(response) {
                 $scope.models.selectedProfiledata = response.data;
                 $scope.stage = 3;
                 console.log($scope.models.selectedProfiledata);
                 console.log($scope.confirmedCode);
               }
            );
        }
      };
      $scope.categoryOptionChanged = function() {
        // TODO: If new category requires filiatory data and the previous category doesn't then
        //       ask for the filiatory data. Change to stage 4 os 5.
        $scope.requiresFiliationData = isFiliationDataFormRequired();
        if ($scope.requiresFiliationData) {
          $scope.stage = 4;
        } else {
          $scope.stage = 5;
        }
      };
      var isFiliationDataFormRequired = function() {
        var profileHasFiliationData = $scope.models.selectedProfiledata.dataFiliation;
        var newCategoryAcceptsFiliation = $scope
          .categories
          .filter(function(x) { return x.id === $scope.models.newCategory.id; })[0]
          .filiationDataRequired;
        return newCategoryAcceptsFiliation && !profileHasFiliationData;
      };
      var getCategories = function() {
        categoriesService
          .getCategories()
          .then(
            function(response) {
              $scope.categories = Object
                .entries(response.categories)
                .filter(function(x){ return x[1].tipo === 1 ;})
                .filter(function(x){ return x[1].isReference ;})
                .map(function(x) {return x[1];});
            }
          );
      };
      $scope.saveProfile = function() {
        alertService.info("Category Changed!!!");
      };
      $scope.getCagetoryName = function(category) {
        var filtered = $scope
          .categories
          .filter(function(x){return x.id === category;});
        if (filtered.length !== 1) {
          alertService.error("The category "+ category+" is not unique or doesn't exists.");
        }
        return filtered[0].name;
      };
      $scope.clearNewCategory = function () {
        $scope.newCategory = undefined;
        $scope.stage = 2;
      };
      getCategories();
    }
    return modifyForensicCategoryController;
  }
);