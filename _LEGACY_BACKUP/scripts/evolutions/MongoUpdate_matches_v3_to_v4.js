var cur = db.matches.find();
while (cur.hasNext()) {
    var match = cur.next();
    var left = db.profiles.find({_id:match.leftProfile.globalCode}).next();
    var right = db.profiles.find({_id:match.rightProfile.globalCode}).next();

    if(typeof(match.leftProfile.categoryId) === "undefined"){
        match.leftProfile.categoryId = left.categoryId;
    }
    if(typeof(match.rightProfile.categoryId) === "undefined"){
        match.rightProfile.categoryId = right.categoryId;
    }
    if(typeof(match.mismatches) === "undefined"){
        var nMismatches = 0;
        for(var i in match.result.matchingAlleles){
            if(match.result.matchingAlleles[i]==="Mismatch"){
                nMismatches++;
            }
        }
        match.mismatches = NumberInt(nMismatches);
    }

    if(typeof(match.lr) === "undefined"){
        match.lr = 0.0;
    }
    db.matches.update({_id: match._id}, match);
}