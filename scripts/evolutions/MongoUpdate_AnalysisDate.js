var cur = db.profiles.find();
while (cur.hasNext()) {
    var profile = cur.next();
    for (var i = 0; i < profile.analyses.length; i++) {
        if (!(profile.analyses[i].date instanceof Date)) {
            profile.analyses[i].date = new Date(profile.analyses[i].date);
        }
    }
    db.profiles.update({_id: profile._id}, profile);
}