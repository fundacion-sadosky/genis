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
      profileDataService,
      profileService
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
      $scope.models = {
        matchingCodesModel: "",
        newCategory: undefined,
        currentCategoryName: undefined,
        selectedProfiledata: {},
        allowedNewCategories: []
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
        // TODO: Check that entered value is not empty
        $scope.confirmedCode = $scope.models.matchingCodesModel;
        if ($scope.confirmedCode !== undefined) {
          // TODO: Should check that the current category can be modified to
          //       the new category.
          profileService
            .isReadOnly($scope.confirmedCode.globalCode)
            .then(
              function(response) {
                $scope.isReadOnly = response;
                if (response.isReadOnly) {
                  alertService.error(response.message);
                  return Promise.reject(response.message);
                }
              }
            )
            .then(
              function() {
                $scope.models.currentCategoryName = $scope
                  .getCategoryName($scope.confirmedCode.category);
                return categoriesService
                  .getCategoryModificationsAllowed(
                    $scope.confirmedCode.category
                  );
              }
            )
            .then(
              function(response) {
                if (response.data.length === 0) {
                  return Promise
                    .reject("La categoría de este perfil no está habilidata para modificarse.");
                }
                $scope.models.allowedNewCategories = response.data;
                return profileDataService
                  .getProfileData($scope.confirmedCode.globalCode);
              }
            )
            .then(
               function(response) {
                 $scope.models.selectedProfiledata = response.data;
                 $scope.stage = 3;
               }
            )
            .catch(
              function(error) {
                alertService.error({message: error});
              }
            );
        }
      };
      $scope.categoryOptionChanged = function() {
        $scope.requiresFiliationData = isFiliationDataFormRequired();
        if ($scope.requiresFiliationData) {
          $scope.stage = 4;
        } else {
          $scope.stage = 5;
        }
      };
      var isFiliationDataFormRequired = function() {
        var profileHasFiliationData = $scope.models
          .selectedProfiledata.dataFiliation;
        var newCategoryAcceptsFiliation = $scope
          .categories
          .filter(
            function(x) { return x.id === $scope.models.newCategory.id; }
          )[0]
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
          (
            $scope.pictures[0] !== undefined &&
            $scope.pictures[0] !== $scope.picturePlaceHolderImage
          ) ||
          (
            $scope.inprints[0] !== undefined &&
            $scope.inprints[0] !== $scope.inprintPrintPlaceHolderImage
          ) ||
          (
            $scope.signatures[0] !== undefined &&
            $scope.signatures[0] !== $scope.signaturePlaceHolderImage
          )
        );
      };

      $scope.saveProfile = function() {
        var dataFiliationDefined = isDataFiliationDefined();
        if (dataFiliationDefined) {
          $scope.profileData.dataFiliation.token = $scope.token;
        } else {
          $scope.profileData.dataFiliation = undefined;
        }
        var noFiliationData = (
          !dataFiliationDefined && !isDataFiliationImages()
        );

        // TODO: Check that the logic here is OK, It is replicated from 
        //       profileDataController.saveProfile method.
        //       I don't think it works as intended.
        if (
          noFiliationData ||
          dataFiliationDefined ||
          isFiliationDataFormRequired()
        ) {
          var updatedProfile = _.cloneDeep($scope.models.selectedProfiledata);
          updatedProfile.category = $scope.models.newCategory.id;
          updatedProfile.dataFiliation = $scope.profileData.dataFiliation;
          profileDataService
            .updateProfileCategoryData(
              $scope.confirmedCode.globalCode,
              updatedProfile
            )
            .then(
              function (response) {
                if (response.data.status === "OK") {
                  alertService.success(
                    {
                      message: 'Se ha actualizado el perfil: ' +
                        $scope.confirmedCode.globalCode
                    }
                  );
                } else {
                  return Promise.reject(response.data.message);
                }
              }
            )
            .catch(
              function (error) { alertService.error({message: error}); }
            );
        } else {
          alertService.error(
            {message: 'Debe completar todos los datos filiatorios o ninguno'}
          );
        }
      };

      $scope.getCategoryName = function(category) {
        var filtered = $scope
          .categories
          .filter(function(x){return x.id === category;});
        if (filtered.length !== 1) {
          alertService.error(
            {message:"La categoría " + category + " no es única o no existe."}
          );
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