/**
 * profiledata service, TODO: exposes user model to the rest of the app.
 */
define([], function() {
  'use strict';

  function ProfileDataService(playRoutes, $log) {

    this.getCategories = function() {
      return playRoutes.controllers.Categories.categoryTree().get();
    };

    this.getSubCategories = function() {
      return playRoutes.controllers.Categories.list().get();
    };

    this.updateProfileData = function(globalCode, profileData){
      return playRoutes.controllers.ProfileData.update(globalCode).put(profileData);
    };

    this.updateProfileCategoryData = function(globalCode, profileData){
      return playRoutes.controllers.ProfileData.modifyCategory(globalCode).put(profileData);
    };

    this.isReadOnly = function(globalCode) {
      return playRoutes.controllers.ProfileData.isReadOnly(globalCode).get();
    };

    this.isEditable = function(globalCode){
      return playRoutes.controllers.ProfileData.isEditable(globalCode).get();
    };

    this.getResource = function(resourceType, id){
      return playRoutes.controllers.ProfileData.getResources(resourceType, id).get();
    };

    this.getCrimeTypes = function() {
      $log.info('calling service: CrimeTypes.list()');
      return playRoutes.controllers.CrimeTypes.list().get();
    };

    this.getBioMaterialTypes = function() {
      $log.info('calling service: BioMaterialTypes.lis()');
      return playRoutes.controllers.BioMaterialTypes.list().get();
    };

    this.getGeneticist = function(lab) {
      $log.info('calling all geneticist for '+lab);
      return playRoutes.controllers.Geneticists.allGeneticist(lab).get();
    };

    this.getGeneticistUsers = function() {
      $log.info('calling all geneticist users');
      return playRoutes.controllers.Geneticists.getGeneticistUsers().get();
    };

    this.getLaboratories = function(){
      $log.info('calling service: all labs');
      return playRoutes.controllers.Laboratories.list().get();
    };

    this.saveProfile = function(profileData) {
      $log.info('calling service: Profiles.create');
      return playRoutes.controllers.ProfileData.create().post(profileData);
    };

    this.getFilesId = function(){
      $log.info('calling for token for images');
      return playRoutes.controllers.Resources.getFilesId().get();
    };

    this.getProfileDataBySampleCode = function(profileId){
      return playRoutes.controllers.ProfileData.getByCode(profileId).get();
    };

    this.getProfileData = function(profileId){
      return playRoutes.controllers.ProfileData.findByCode(profileId).get();
    };

    this.getProfilesData = function(globalCodes){
      return playRoutes.controllers.ProfileData.findByCodes(globalCodes).get();
    };

    this.getMotive = function(sampleCode){
      return playRoutes.controllers.ProfileData.getDeleteMotive(sampleCode).get();
    };

    this.deleteProfile = function(sampleCode,motive){
      return playRoutes.controllers.ProfileData.deleteProfile(sampleCode).put(motive);
    };

    this.isModal = function(){return false;};

    this.getResourcesURL = function(){return '/resources/static';};
  }

  return ProfileDataService;

});
