/**
 * profiledata controllers.
 */
define([], function() {
  'use strict';

  function ProfileDataController($scope, $log, profileDataService, profileDataCommonService, $filter, $routeParams, alertService, $modalInstance) {

    $scope.mode = 'add';
    $scope.isModal = profileDataService.isModal();

    $scope.picturePlaceHolderImage = 'assets/images/default-user.png';
    $scope.inprintPrintPlaceHolderImage = 'assets/images/fingerprint.jpg';
    $scope.signaturePlaceHolderImage = 'assets/images/signature.jpg';

    $scope.inprints = [];
    $scope.pictures = [];
    $scope.signatures = [];
    $scope.categories = {};

    $scope.inNameOf = false;

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

    profileDataCommonService.getCategories().then(function(response) {
      $scope.categories = response;
    });

    if($routeParams.samplecode) {
      profileDataService.isEditable($routeParams.samplecode).then(function(response){
        if((JSON.parse(response.data) === true)) {
          $scope.mode = 'update';
        } else {
          $scope.mode = 'readonly';
        }
      });

      profileDataService.getProfileDataBySampleCode($routeParams.samplecode).then(function(response){
        $scope.profileData = response.data;
        //$scope.inNameOf = $scope.isFieldComplete($scope.profileData.laboratory);

        //obtener los genetistas
        $scope.inNameOf = !!response.data.responsibleGeneticist;
        response.data.responsibleGeneticist = + response.data.responsibleGeneticist;

        profileDataService.getGeneticist(response.data.laboratory).then(function(response) {
          $scope.geneticists = response.data;
        });

        if(response.data.dataFiliation){

          if(response.data.dataFiliation.inprints.length !== 0){
            $scope.inprints.shift();
          }

          if(response.data.dataFiliation.pictures.length !== 0){
            $scope.pictures.shift();
          }

          if(response.data.dataFiliation.inprints.length !== 0){
            $scope.inprints.shift();
          }

          if(response.data.dataFiliation.signatures.length !== 0){
            $scope.signatures.shift();
          }

          if(response.data.dataFiliation.birthday === -62135758800000){
            $scope.profileData.dataFiliation.birthday = undefined;
          }

          response.data.dataFiliation.inprints.forEach( function(inprint){
            $scope.inprints.push(profileDataService.getResourcesURL() + "/I/" +inprint);
          });


          response.data.dataFiliation.pictures.forEach( function(picture){
            $scope.pictures.push(profileDataService.getResourcesURL() + "/P/" + picture);
          });

          response.data.dataFiliation.signatures.forEach( function(picture){
            $scope.signatures.push(profileDataService.getResourcesURL() + "/S/" + picture);
          });

        }

      });

    } else {
      $scope.profileData = {globalCode : ""};
    }


    $scope.showFiliationDataSection = function() {


      if( $scope.profileData && $scope.profileData.category ){

        for(var j in $scope.categories ){

          var subcats = $scope.categories[j].subcategories;

          for(var i in subcats){
            if( subcats[i].id === $scope.profileData.category ){
              return subcats[i].filiationDataRequired;
            }
          }
        }
        return false;

      }else{
        return false;
      }
    };

    $scope.hasMandatoryData = function() {
      return $scope.profileData &&
        $scope.profileData.internalSampleCode &&
        $scope.profileData.assignee &&
        $scope.profileData.category;
    };

    function closeModal() {$modalInstance.close();}

    function onSucessUpdate() {
      if ($scope.isModal){
        closeModal();
      }
    }

    $scope.saveProfile = function() {

      //$scope.profileData.globalCode = "";

      if($scope.inNameOf && !$scope.profileData.responsibleGeneticist){

        alertService.error({message: 'No ha ingresado el Genetista responsable'});
        return;
      }


      if($scope.profileData.sampleEntryDate){
        $scope.profileData.sampleEntryDate = $filter('date')(new Date($scope.profileData.sampleEntryDate), 'yyyy-MM-dd');
      }

      if($scope.profileData.profileExpirationDate){
        $scope.profileData.profileExpirationDate = $filter('date')(new Date($scope.profileData.profileExpirationDate), 'yyyy-MM-dd');
      }

      if($scope.profileData.sampleDate){
        $scope.profileData.sampleDate = $filter('date') (new Date($scope.profileData.sampleDate), 'yyyy-MM-dd');
      }

      var isDataFiliationDefined = $scope.isObjectDefined($scope.profileData.dataFiliation);
      var isDataFiliationComplete = false;
      var isDataFiliationImages = false;

      isDataFiliationImages = $scope.showFiliationDataSection() && ( ($scope.pictures[0] !== undefined && $scope.pictures[0] !== $scope.picturePlaceHolderImage) || ($scope.inprints[0] !== undefined && $scope.inprints[0] !== $scope.inprintPrintPlaceHolderImage) || ($scope.signatures[0] !== undefined && $scope.signatures[0] !== $scope.signaturePlaceHolderImage));

      if (isDataFiliationDefined) {
        isDataFiliationComplete = $scope.isFieldComplete($scope.profileData.dataFiliation.fullName) && $scope.isFieldComplete($scope.profileData.dataFiliation.nickname) &&
          $scope.isFieldComplete($scope.profileData.dataFiliation.birthday) && $scope.isFieldComplete($scope.profileData.dataFiliation.birthPlace) &&
          $scope.isFieldComplete($scope.profileData.dataFiliation.nationality) && $scope.isFieldComplete($scope.profileData.dataFiliation.identification) &&
          $scope.isFieldComplete($scope.profileData.dataFiliation.identificationIssuingAuthority) && $scope.isFieldComplete($scope.profileData.dataFiliation.address);

        //if (isDataFiliationComplete){
        //    $scope.profileData.dataFiliation.token = $scope.token;
        //}

        isDataFiliationComplete=true;
        $scope.profileData.dataFiliation.token = $scope.token;

      } else {
        $scope.profileData.dataFiliation = undefined;
      }

      if ((!isDataFiliationDefined && !isDataFiliationImages) || (isDataFiliationDefined && isDataFiliationComplete)) {
        if ($scope.mode === 'add') {
          profileDataService.saveProfile($scope.profileData).then(
            function (response) {
              if (response) {
                $scope.sampleCode = response.data.sampleCode;
                alertService.success({message: 'Código de muestra generado: ' + $scope.sampleCode});
              }
              $scope.cancel();
            },
            function (response) {
              if (response.data && response.data.message) {
                alertService.error({message: response.data.message});
              } else {
                alertService.error({message: 'Ha ocurrido un error al crear'});
              }
            }
          );
        } else {
          profileDataService.updateProfileData($routeParams.samplecode,$scope.profileData).then(
            function (response) {
              if(response.data){
                $scope.sampleCode = (response.data === true) ? $routeParams.samplecode : '';
                alertService.success({message: 'Se ha actualizado el perfil: ' + $scope.sampleCode});
                onSucessUpdate();
              }else{
                alertService.error({message: 'Ha ocurrido un error al actualizar'});
              }
            },
            function (response) {
              console.log('Error al actualizar el perfil',response);
              alertService.error({message: 'Ha ocurrido un error al actualizar'});
            }
          );
        }
      } else {
        alertService.error({message: 'Debe completar todos los datos filiatorios o ninguno'});
      }
    };

    $scope.isFieldComplete = function (field) {
      return field !== undefined && field !== "";
    };

    $scope.isObjectDefined = function(object) {
      for (var property in object){
        if (object[property] !== undefined && object[property] !== null){
          return true;
        }
      }

      return false;
    };

    $scope.token = {};

    $scope.cancel = function(){
      if ($scope.isModal){
        closeModal();
      } else {
        if($scope.mode === "add"){
          if($scope.pdform.$pristine) {
            window.history.back();
          } else {
            $scope.profileData = {};
            $scope.inprints = [];
            $scope.pictures = [];
            $scope.signatures = [];
            $scope.inNameOf = undefined;
            $scope.pdform.$setPristine();
          }
        }else{
          window.history.back();
        }
      }
    };

  }

  return ProfileDataController;

});