define([], function() {
    'use strict';

    function InboxService(playRoutes) {

        this.count = function(searchObject){
            return playRoutes.controllers.Notifications.count().post(searchObject);
        };
        
        this.search = function(searchObject){
            return playRoutes.controllers.Notifications.search().post(searchObject);
        };

        this.changeFlag = function(id, flagged){
            return playRoutes.controllers.Notifications.changeFlag(id, flagged).post();
        };

        this.delete = function(id){
            return playRoutes.controllers.Notifications.delete(id).delete();
        };

    }

    return InboxService;
});