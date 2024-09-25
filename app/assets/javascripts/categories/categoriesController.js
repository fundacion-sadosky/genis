define(['angular', 'jquery','lodash'], function(ng, $,_) {
  'use strict';

  function CategoriesCtrl ($scope, categoriesService, $modal, alertService, analysisTypeService) {

    $scope.tabs = [{active: true}, {active: false}, {active: false}];

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

    analysisTypeService.listById().then(function (response) {
      $scope.analysisTypes = response;
      $scope.activeAnalysis = parseInt(Object.keys($scope.analysisTypes)[0]);
    });
    $scope.isCatModVisible = false;
    $scope.isCatGrpEditVisible = true;
    $scope.categoryModificationValues = {};
    $scope.categoryModifications = [];
    $scope.innvCategories = [];
    function loadCategoryModifications() {
      categoriesService
        .getCategoryModifications()
        .then(
          function(response) {
            $scope.categoryModifications = response.data.map(
              function (mod) {
                return [mod.from, mod.to];
              }
            );
          }
        );
    }
    function getUndoubtedlyCategories() {
      $scope.innvCategories = Object
        .entries($scope.categories)
        .map(function(x) {return x[1];})
        .filter( function(cat) { return cat.tipo === 1 && cat.isReference; } );
    }
    $scope.isCategoryModificationRowVisible = false;
    $scope.showCategoryModifications = function() {
      loadCategoryModifications();
      getUndoubtedlyCategories();
      $scope.isCatModVisible = true;
      $scope.isCatGrpEditVisible = false;
      $scope.isCategoryModificationRowVisible = false;
    };
    $scope.setCategoryModification = function() {
      $scope.isCategoryModificationRowVisible = true;
    };
    $scope.addCategoryModification = function() {
      categoriesService
        .registerCategoryModification(
          $scope.categoryModificationValues.from,
          $scope.categoryModificationValues.to
        )
        .then(
          function(response) {
            if (response.data.status === "success") {
              alertService.success({message: response.data.message});
              loadCategoryModifications();
            } else {
              alertService.error({message: response.data.message});
            }
          }
        );
      $scope.isCategoryModificationRowVisible = false;
    };
    $scope.deleteModification = function(mod) {
      categoriesService
        .unregisterCategoryModification(mod[0], mod[1])
        .then(
          function(response) {
            if (response.data.status === "success") {
              alertService.success({message: response.data.message});
              loadCategoryModifications();
            } else {
              alertService.error({message: response.data.message});
            }
          }
        );
    };
    function firstKey(obj) {
      for (var k in obj) {return k;}
    }

    $scope.selectGrp = function(grpId) {
      $scope.isCatGrpEditVisible = true;
      $scope.isCatModVisible = false;
      if(grpId){
        $scope.currGrpId = grpId;
        $scope.groups[$scope.currGrpId].isOpen = true;
        $scope.currCatId = $scope.groups[$scope.currGrpId].categories && $scope.groups[$scope.currGrpId].categories[0];
        $scope.tabs[0].active = true;
      }
    };

    $scope.addGroup = function() {
      $modal.open({
        templateUrl:'/assets/javascripts/categories/groupModal.html',
        controller : 'groupModalController',
        resolve : {
          group: function() {
            return {};
          },
          mode: function() {
            return 'add';
          }
        }
      }).result.then(addGroup);
    };

    $scope.editGroup = function(grpId, event) {
      console.log(event);
      //event.stopPropagation();
      $modal.open({
        templateUrl:'/assets/javascripts/categories/groupModal.html',
        controller : 'groupModalController',
        resolve : {
          group: function() {
            return ng.copy($scope.groups[grpId]);
          },
          mode: function() {
            return 'edit';
          }
        }
      }).result.then(function(result) {
        if(typeof result === "object") {
          updateGroup(result);
        } else {
          removeGroup(result);
        }
      });
    };

    function addGroup() {
      getCategories();
      alertService.success({message: 'El grupo ha sido creado'});
    }

    function updateGroup(group) {
      $scope.groups[group.id] = group;
      alertService.success({message: 'El grupo ha sido actualizado'});
    }

    function removeGroup(groupId) {
      delete $scope.groups[groupId];
      $scope.selectGrp(firstKey($scope.groups));
      alertService.success({message: 'El grupo ha sido eliminado'});
    }

    function selectCat(catId) {
      $scope.isCatGrpEditVisible = true;
      $scope.isCatModVisible = false;
      formReset();
      $scope.currCatId = catId;
      if ($scope.analysisTypes) {
        $scope.activeAnalysis = parseInt(Object.keys($scope.analysisTypes)[0]);
        $scope.tabs[0].active = true;
      }
    }

    $scope.selectCat = function(catId, event) {
      event.stopPropagation();
      selectCat(catId);
    };

    $scope.addCategory = function(event) {
      event.stopPropagation();
      $modal.open({
        templateUrl:'/assets/javascripts/categories/categoryModal.html',
        controller : 'categoryModalController',
        resolve : {
          category: function() {
            return {group: $scope.currGrpId};
          },
          mode: function() {
            return 'add';
          }
        }
      }).result.then(addCategory);
    };

    $scope.editCategory = function(catId) {
      $modal.open({
        templateUrl:'/assets/javascripts/categories/categoryModal.html',
        controller : 'categoryModalController',
        resolve : {
          category: function() {
            return $scope.categories[catId];
          },
          mode: function() {
            return 'edit';
          }
        }
      }).result.then(function(result) {
        if(typeof result === "object") {
          updateCategory(result);
        } else {
          removeCategory(result);
        }
      });
    };

    $scope.save = function() {
      var category = $scope.categories[$scope.currCatId];
      categoriesService.updateFullCategory(category).then(
        function(){
          alertService.success({message: 'Los cambios se han actualizado'});
          formReset();
          refreshCategories();
        },
        function(response){
          alertService.error({message: response.data});
        });
    };

    $scope.cancel = function() {
      formReset();
      getCategories();
    };

    function formReset(){
      $scope.$broadcast('form-reset');
    }

    function addCategory(category) {
      getCategories().then(function() {
        $scope.currCatId = category.id;
      });

      alertService.success({message: 'La categoría ha sido creada'});
    }

    function updateCategory(category) {
      $scope.categories[category.id] = category;
      alertService.success({message: 'La categoría ha sido actualizada'});
    }

    function removeCategory(catId) {
      var group = $scope.groups[$scope.currGrpId];
      var index = group.categories.indexOf(catId);
      group.categories.splice(index, 1);
      console.log(catId);
      delete $scope.categories[catId];

      for (var i = 0; i < $scope.catArray.length; i++) {
        var category = $scope.catArray[i];
        if(category.id === catId) {
          $scope.catArray.splice(i, 1);
          break;
        }
      }

      getCategories();
      alertService.success({message: 'La categoría ha sido eliminada'});
    }

    function getCategories(){
      return categoriesService.getCategories().then(function(response) {
        $scope.groups = response.groups;

        $scope.mpi = {} ;
        $scope.mpi.AM = $scope.groups.AM;
        $scope.mpi.PM = $scope.groups.PM;

        $scope.forense = {};

        _.forEach($scope.groups, function(value, key) {

          if (key!== "AM" && key!== "AM_DVI" && key!== "PM" && key!== "PM_DVI" )
          { var n = key;
            $scope.forense[n] = value;}

        });

        $scope.dvi = {} ;
        $scope.dvi.AM_DVI = $scope.groups.AM_DVI;
        $scope.dvi.PM_DVI = $scope.groups.PM_DVI;

        $scope.categories = response.categories;

        for (var categoryId in $scope.categories) {
          var category = response.categories[categoryId];
          var group = $scope.groups[category.group];
          if (category.pedigreeAssociation === true) {
            group.pedigreeAssociation = true;
          } else {
            group.pedigreeAssociation = false;
          }
        }

        if ($scope.currGrpId) {
          $scope.selectGrp($scope.currGrpId);
        } else {
          $scope.selectGrp(firstKey($scope.groups));
        }

        if ($scope.currCatId) {selectCat($scope.currCatId, false);}

        $scope.catArray = [];
        for (var catId in $scope.categories) {
          var cat = $scope.categories[catId];
          cat.id = catId;
          if($scope.analysisTypes){
            for (var i = 0; i <  Object.keys($scope.analysisTypes).length; i++) {
              var id = parseInt(Object.keys($scope.analysisTypes)[i]);
              var exists = cat.configurations.hasOwnProperty(id);
              if (!exists) {
                cat.configurations[id] = {collectionUri: '', draftUri: '',
                  minLocusPerProfile: 'K', maxOverageDeviatedLoci: '0', 'maxAllelesPerLocus': 6};
              }
            }
          }

          $scope.catArray.push(cat);
        }
      });
    }
    function refreshCategories(){
      return categoriesService.getCategories().then(function(response) {

        $scope.categoriesTmp = response.categories;
        for (var catId in $scope.categoriesTmp) {
          var cat = $scope.categoriesTmp[catId];
          var cat2 = $scope.categories[catId];
          cat2.matchingRules=cat.matchingRules;
        }
      });
    }
    getCategories();

    $scope.selectAnalysis = function(id) {
      if ($scope.activeAnalysis !== id) {
        $scope.activeAnalysis = id;
        formReset();
        $scope.fadeIn("categories-tabs");
      }

      if (id === 4){
        $scope.tabs[2].active = true;
      } else {
        $scope.tabs[0].active = true;
      }
    };

    $scope.fadeIn = function(id) {
      $("#" + id).hide();
      $("#" + id).fadeIn();
    };
    
  }

  return CategoriesCtrl;

});