package com.clou.ess;

import com.clou.ess.model.BmsModel;
import com.clou.ess.model.PackageModel;
import com.clou.ess.service.BmsMongoService;
import com.clou.ess.service.PackageMongoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EssApiApplicationTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    BmsMongoService bmsRepository;

    @Autowired
    PackageMongoService packageMongoService;
    @Test
    public void contextLoads() {
        /*Criteria criteria = new Criteria();
        criteria.and("bms_code").is("LFT-C1-0515-01");
        Query query = new Query(criteria);
        query.limit(1);
        query.with(new Sort(Sort.Direction.DESC, "_id"));

        long           startTime = System.currentTimeMillis();
        List<BmsModel> list      = bmsMongoService.findByProp("LFT-C1-0515-01", "2018-03-19 00:00:00", "2018-03-19 01:59:59");
        long           endTime   = System.currentTimeMillis();
        System.out.println("耗时: " + (endTime - startTime));
        for (BmsModel model : list) {
            System.out.println(model);
        }*/

        /*Map<String, Object> fields = new HashMap<>();
        for (int i = 1; i <= 30; i++) {
            fields.put("package_data_" + i, 0);
        }
        DBObject project = new BasicDBObject();
        project.put("$project", fields);

        String   matchStr = "{$match : {\"cluster_code\" : \"LFT-C1-0515-01-BC01\"}}";
        DBObject match    = (DBObject) JSON.parse(matchStr);

        DBObject limit = new BasicDBObject();
        limit.put("$limit", 1);

        List<DBObject> dbObjectList = new ArrayList<>();
        dbObjectList.add(project);
        dbObjectList.add(match);
        dbObjectList.add(limit);

        AggregationOutput output = mongoTemplate.getCollection("LFT-C1-0515-01_clusterAndpackage").aggregate(dbObjectList);

        for (DBObject cluster : output.results()) {
            System.out.println(cluster);
        }*/

        PackageModel packageModel = packageMongoService.findOnePackage(1,"LFT-C1-0515-01-BC01", 1, "2018-03-20 15:59:00", "2018-03-20 16:00:00");
        /*BeanWrapper beanWrapper  = new BeanWrapperImpl(packageModel);
        beanWrapper.setPropertyValues(m);
*/
        System.out.println(packageModel);

        BmsModel model = bmsRepository.findOne(1,"LFT-C1-0515-01", "2018-03-20 15:59:00", "2018-03-20 16:00:00");
        System.out.println(model);

    }

}