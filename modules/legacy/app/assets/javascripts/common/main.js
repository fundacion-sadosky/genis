define(['angular', './services/cryptoService', './directives/showPermissions', './directives/enablePermissions','./directives/hidePermissions', './services/helper', './services/playRoutes', './services/resourcesHelper', './filters',
        './directives/example', './directives/validation', './directives/helptip', './directives/modal-img', 
        './directives/pdg-locus', './directives/pdg-allele-label-picker',
        './directives/pdg-gen-popover', 
        './directives/pdg-confirm',
        './directives/checklist',
        './services/alertService',
        './directives/defaultValue',
        './directives/tagsInput',
        './directives/tagsInputModal',
        './directives/pdg-sortable',
        './directives/formValidation',
        './directives/pdg-gen-popover-mt','./directives/pdg-locus-range-mt'],
    function(angular, CryptoService, showPermissions, enablePermissions,hidePermissions) {
  'use strict';

  var mod = angular.module('pdg.common', ['common.helper', 'common.playRoutes', 'common.resourcesHelper', 'common.filters',
    'common.directives.example', 'common.directives.modalImg', 'common.directives.helptip', 'common.directives', 
    'common.directives.pdgLocus', 'common.directives.pdgAlleleLabelPicker', //'common.directives.pdgStatisticalOptions', 
    /*'common.directives.pdgProfileDataInfo', */'common.directives.pdgGenPopover', //'common.directives.pdgProfileDataInfoPopover', 
    'common.directives.pdgConfirm','common.directives.cheklist-model', 'common.alertService', 'common.directives.defaultValue',
    'common.directives.tagsInput','common.directives.tagsInputModal', 'common.directives.pdgSortable', 'common.directives.formValidation', 'common.directives.pdgGenPopoverMt','common.directives.pdgLocusRangeMt']);
  
  mod.service('cryptoService', ['userService', CryptoService]);
  mod.directive('showPermissions', ['userService', showPermissions]);
  mod.directive('hidePermissions', ['userService', hidePermissions]);
  mod.directive('enablePermissions', ['userService', enablePermissions]);
  
  return mod;
});
