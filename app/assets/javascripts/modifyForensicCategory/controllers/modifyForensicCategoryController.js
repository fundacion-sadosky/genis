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
        currentCategoryName: undefined,
        selectedProfiledata: {}
      };
      $scope.matchingCodes = [];
      $scope.categories = [];
      $scope.stage = 1;
      $scope.confirmedCode = undefined;
      $scope.profileData = {};

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
                $scope.models.matchingCodesModel = $scope.matchingCodes[0];
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
          $scope.models.currentCategoryName = $scope.getCategoryName($scope.confirmedCode.category);
          getProfileData($scope.confirmedCode.globalCode)
            .then(
               function(response) {
                 $scope.models.selectedProfiledata = response.data;
                 $scope.stage = 3;
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
              $scope.models.newCategory = $scope.categories[0];
            }
          );
      };

      $scope.isFieldComplete = function (field) {
        return field !== undefined && field !== "";
      };

      $scope.isObjectDefined = function(object) {
        for (var property in object){
          if (object[property] !== undefined && object[property] !== null) {
            return true;
          }
        }
        return false;
      };

      var isDataFiliationDefined = function() {
        return $scope.isObjectDefined($scope.profileData.dataFiliation);
      };

      var isDataFiliationImages = function() {
        return (
          ($scope.pictures[0] !== undefined && $scope.pictures[0] !== $scope.picturePlaceHolderImage) ||
          ($scope.inprints[0] !== undefined && $scope.inprints[0] !== $scope.inprintPrintPlaceHolderImage) ||
          ($scope.signatures[0] !== undefined && $scope.signatures[0] !== $scope.signaturePlaceHolderImage)
        );
      };

      $scope.saveProfile = function() {
        var dataFiliationDefined = isDataFiliationDefined();
        if (dataFiliationDefined) {
          $scope.profileData.dataFiliation.token = $scope.token;
        } else {
          $scope.profileData.dataFiliation = undefined;
        }
        var noFiliationData = (!dataFiliationDefined && !isDataFiliationImages());

        // TODO: Check that the logic here is OK, It is replicated from profileDataController.saveProfile method.
        //       I don't think it works as intended.
        if (noFiliationData || dataFiliationDefined) {
          profileDataService
            .updateProfileData($scope.confirmedCode, $scope.profileData)
            .then(
              function (response) {
                if (response.data) {
                  alertService.success({message: 'Se ha actualizado el perfil: ' + $scope.confirmedCode});
                } else {
                  return Promise.reject("Ha ocurrido un error al actualizar");
                }
              }
            )
            .catch( function (error) { alertService.error({message: error}); } );
        } else {
          alertService.error({message: 'Debe completar todos los datos filiatorios o ninguno'});
        }
      };

      $scope.getCategoryName = function(category) {
        var filtered = $scope
          .categories
          .filter(function(x){return x.id === category;});
        if (filtered.length !== 1) {
          alertService.error({message:"The category " + category + " is not unique or doesn't exists."});
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