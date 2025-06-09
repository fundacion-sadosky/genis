define(
    [],
    function() {
        'use strict';
        function DesktopSearchController ($scope, matchesService, notificationsService, profileId, profiledataService) {

            var inicializar = function () {
                $scope.matches = [];
                $scope.stall = false;
            };
            inicializar();

            notificationsService.onMatchStatusNotification(function(msg){
                var status = msg.status;
                if (status === "started"){
                    $scope.stall = false;
                    $scope.working = true;
                    $scope.fail = false;
                    console.log("Match started");
                }else if (status === "ended"){
                    $scope.stall = true;
                    $scope.working = false;
                    $scope.fail = false;
                    console.log("Match ended");
                }else if (status === "fail"){
                    $scope.stall = false;
                    $scope.working = false;
                    $scope.fail = true;
                    console.log("Match failed");
                }else if (status === "pedigreeStarted"){
                    $scope.pedigreeStall = false;
                    $scope.pedigreeWorking = true;
                    console.log("Pedigree Match started");
                } else if (status === "pedigreeEnded"){
                    $scope.pedigreeStall = true;
                    $scope.pedigreeWorking = false;
                    console.log("Pedigree Match ended");
                }

                $scope.$apply();

            });

            notificationsService.onNotification(function(msg){
                    if (msg.kind === 'matching'){
                        var url = msg.url;
                        var parts = url.split('/');  // in an url like /comparison/<profile id>/matchedProfileId/<matched profile id>/matchingId/<matching id>
                        $scope.profileId  = parts[2];
                        var matchedProfileId = parts[4];
                        $scope.matches.push(matchedProfileId);
                        console.log("New match found:", matchedProfileId);

                        $scope.profileData = profiledataService.getProfileDataBySampleCode(profileId);
                        $scope.matchedProfileData = profiledataService.getProfileDataBySampleCode(matchedProfileId);

                    }
                });

                $scope.printReport = function(matchedProfileId) {
                $scope.matchedProfileId = matchedProfileId;
                var head = '<head><title>Comparación</title>';
                $("link").each(function () {
                    head += '<link rel="stylesheet" href="' + $(this)[0].href + '" />';
                });
                head += "</head>";
                $scope.$apply();
                var report = window.open('', '_blank');
                report.document.write(
                    '<html>' + head +
                    '<body>' +
                    $('#report').html() +
                    '</body></html>'
                );
                report.document.close();
                $(report).on('load', function(){
                    report.print();
                    report.close();
                });
            };


            $scope.cancel = function () {
                $scope.$dismiss('cancel');
            };

        }
        return DesktopSearchController;
    });