define([],function() {
    'use strict';

    function ProfileExporterToLimsService(playRoutes,userService) {

        this.exporterArchivesLims = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            return playRoutes.controllers.Profiles.exporterLimsFiles().post(search);
        };

    }

    return ProfileExporterToLimsService;

});