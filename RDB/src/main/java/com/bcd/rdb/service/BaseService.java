package com.bcd.rdb.service;

import com.bcd.base.i18n.I18NData;
import com.bcd.rdb.anno.Unique;
import com.bcd.base.exception.BaseRuntimeException;
import com.bcd.base.condition.Condition;
import com.bcd.rdb.bean.info.BeanInfo;
import com.bcd.rdb.jdbc.rowmapper.MyColumnMapRowMapper;
import com.bcd.rdb.util.ConditionUtil;
import com.bcd.rdb.repository.BaseRepository;
import com.bcd.rdb.util.RDBUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Administrator on 2017/4/11.
 */
@SuppressWarnings("unchecked")
public class BaseService<T,K extends Serializable> {
    @PersistenceContext
    public EntityManager em;

    @Autowired
    public BaseRepository<T,K> repository;

    @Autowired
    public JdbcTemplate jdbcTemplate;

    private volatile BeanInfo beanInfo;


    /**
     * 获取当前service对应实体类的信息
     * @return
     */
    public BeanInfo getBeanInfo(){
        if(beanInfo==null){
            synchronized (this){
                if(beanInfo==null){
                    Class beanClass=(Class <T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
                    beanInfo=BeanInfo.getBeanInfo(beanClass);
                }
            }
        }
        return beanInfo;
    }

    public boolean existsById(K id){
        return repository.existsById(id);
    }

    public long count(){
        return repository.count();
    }

    public long count(Condition condition){
        Specification<T> specification= ConditionUtil.toSpecification(condition);
        return repository.count(specification);
    }

    public List<T> findAll(){
        return repository.findAll();
    }

    public List<T> findAll(Condition condition){
        Specification<T> specification= ConditionUtil.toSpecification(condition);
        return repository.findAll(specification);
    }

    public List<T> findAll(Condition condition, Sort sort){
        Specification<T> specification= ConditionUtil.toSpecification(condition);
        return repository.findAll(specification,sort);
    }

    public Page<T> findAll(Pageable pageable){
        return repository.findAll(pageable);
    }

    public Page<T> findAll(Condition condition, Pageable pageable){
        Specification<T> specification= ConditionUtil.toSpecification(condition);
        return repository.findAll(specification,pageable);
    }

    public List<T> findAll(Sort sort){
        return repository.findAll(sort);
    }

    public List<T> findAllById(Iterable<K> iterable){
        return repository.findAllById(iterable);
    }

    public List<T> findAllById(K[] kArr){
        return repository.findAllById(Arrays.asList(kArr));
    }

    public T findById(K k){
        return repository.findById(k).orElse(null);
    }


    public T findOne(Condition condition){
        Specification<T> specification= ConditionUtil.toSpecification(condition);
        return repository.findOne(specification).orElse(null);
    }



    @Transactional
    public T save(T t){
        validateUniqueBeforeSave(t);
        return repository.save(t);
    }

    @Transactional
    public List<T> saveAll(Iterable<T> iterable){
        validateUniqueBeforeSave(iterable);
        return repository.saveAll(iterable);
    }

    /**
     * 逻辑为
     * 先查询出数据库对应记录
     * 然后将参数对象非空值注入到数据库对象中
     * 保存对象
     *
     * 对象t主键必须为'id'且必须属于BeanUtil.BASE_DATA_TYPE
     *
     * 会改成传入参数的值
     *
     * @param t
     */
    @Transactional
    public T saveIgnoreNull(T t){
        T returnVal;
        Object val= RDBUtil.getPKVal(t);
        if(val==null){
            returnVal=save(t);
        }else{
            T dbt=findById((K)val);
            try {
                BeanUtils.copyProperties(dbt,t);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw BaseRuntimeException.getException(e);
            }
            returnVal=save(t);
        }
        return returnVal;
    }

    /**
     * 逻辑为
     * 先查询出数据库对应记录
     * 然后将参数对象非空值注入到数据库对象中
     * 保存数据库对象
     *
     * 对象t主键必须为'id'且必须属于BeanUtil.BASE_DATA_TYPE
     *
     * 会改变传入参数的值
     *
     * @param iterable
     */
    @Transactional
    public List<T> saveIgnoreNull(Iterable<T> iterable){
        List<T> returnVal=new ArrayList<>();
        Iterator<T> it= iterable.iterator();
        while(it.hasNext()){
            T t=it.next();
            returnVal.add(saveIgnoreNull(t));
        }
        return returnVal;
    }

    /**
     * 此操作不会触发检查
     */
    @Transactional
    public void deleteAll(){
        repository.deleteAll();
    }

    @Transactional
    public void deleteById(K ... ids){
        for (K id : ids) {
            repository.deleteById(id);
        }
    }

    @Transactional
    public void delete(T t){
        repository.delete(t);
    }

    @Transactional
    public void deleteAll(Iterable<T> iterable){
        repository.deleteAll(iterable);
    }

    /**
     * 此操作不会触发检查
     */
    @Transactional
    public void deleteAllInBatch(){
        repository.deleteAllInBatch();
    }

    @Transactional
    public void deleteInBatch(Iterable<T> iterable){
        repository.deleteInBatch(iterable);
    }





    /**
     * 优于普通删除方法
     * @param condition
     * @return 删除的记录条数
     */
    @Transactional
    public int delete(Condition condition){
        Specification specification= ConditionUtil.toSpecification(condition);
        CriteriaBuilder criteriaBuilder= em.getCriteriaBuilder();
        CriteriaQuery criteriaQuery= criteriaBuilder.createQuery(getBeanInfo().clazz);
        CriteriaDelete criteriaDelete= criteriaBuilder.createCriteriaDelete(getBeanInfo().clazz);
        Predicate predicate= specification.toPredicate(criteriaDelete.from(getBeanInfo().clazz),criteriaQuery,criteriaBuilder);
        criteriaDelete.where(predicate);
        return em.createQuery(criteriaDelete).executeUpdate();
    }


    /**
     * 优于普通更新方法
     * @param condition
     * @param attrMap 更新的字段和值的map
     * @return 更新的记录条数
     */
    @Transactional
    public int update(Condition condition, Map<String,Object> attrMap){
        if(attrMap==null||attrMap.size()==0){
            return 0;
        }
        Specification specification= ConditionUtil.toSpecification(condition);
        CriteriaBuilder criteriaBuilder= em.getCriteriaBuilder();
        CriteriaQuery criteriaQuery= criteriaBuilder.createQuery(getBeanInfo().clazz);
        CriteriaUpdate criteriaUpdate= criteriaBuilder.createCriteriaUpdate(getBeanInfo().clazz);
        Predicate predicate= specification.toPredicate(criteriaUpdate.from(getBeanInfo().clazz),criteriaQuery,criteriaBuilder);
        criteriaUpdate.where(predicate);
        Iterator<Map.Entry<String,Object>> it= attrMap.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String,Object> entry= it.next();
            criteriaUpdate.set(entry.getKey(),entry.getValue());
        }
        return em.createQuery(criteriaUpdate).executeUpdate();
    }

    /**
     * 执行native sql
     * query.getResultList() 结果类型为 List<Map>
     * @param sql
     * @return
     */
    @Transactional
    public Query executeNativeSql(String sql,Object ... params){
        Query query= em.createNativeQuery(sql);
        //设置返回的结果集为List<Map>形式;如果不设置,则默认为List<Object[]>
        query.unwrap(NativeQueryImpl.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        if(params!=null) {
            for (int i = 0; i <= params.length-1; i++) {
                query.setParameter(i+1, params[i]);
            }
        }
        return query;
    }

    /**
     * 分页查询
     * @param countSql 统计数量sql
     * @param sql 查询结果集sql(不带limit)
     * @param pageable 分页对象参数
     * @param params 参数(用于countSql和sql)
     * @return
     */
    public Page<Map<String,Object>> queryByNativeSql(String countSql,String sql,Pageable pageable,Object ... params){
        Integer count= jdbcTemplate.queryForObject(countSql,Integer.class,params);
        if(count==null||count==0){
            return new PageImpl<>(new ArrayList<>(),pageable,0);
        }else{
            String limitSql=sql+"\n limit ?,?";
            Object[] limitParams;
            if(params==null||params.length==0){
                limitParams=new Object[2];
            }else{
                limitParams=Arrays.copyOf(params,params.length+2);
            }
            limitParams[limitParams.length-2]=pageable.getPageNumber() * pageable.getPageSize();
            limitParams[limitParams.length-1]=pageable.getPageSize();
            List<Map<String,Object>> dataList= jdbcTemplate.query(limitSql,MyColumnMapRowMapper.ROW_MAPPER,limitParams);
            return new PageImpl<>(dataList,pageable,count);
        }
    }

    /**
     * 分页查询并转换为实体类
     * @param sql
     * @param pageable
     * @param clazz
     * @param params
     * @return
     */
    public Page<T> queryByNativeSql(String countSql,String sql,Pageable pageable,Class<T> clazz,Object ... params){
        Integer count= jdbcTemplate.queryForObject(countSql,Integer.class,params);
        if(count==0){
            return new PageImpl<>(new ArrayList<>(),pageable,0);
        }else{
            String limitSql=sql+"\n limit ?,?";
            Object[] limitParams;
            if(params==null||params.length==0){
                limitParams=new Object[2];
            }else{
                limitParams=Arrays.copyOf(params,params.length+2);
            }
            limitParams[limitParams.length-2]=pageable.getPageNumber() * pageable.getPageSize();
            limitParams[limitParams.length-1]=pageable.getPageSize();
            List<T> dataList= jdbcTemplate.query(limitSql,new BeanPropertyRowMapper<>(clazz),limitParams);
            return new PageImpl<>(dataList,pageable,count);
        }
    }

    /**
     * 字段唯一性验证
     *
     * 对象t主键必须为'id'且必须属于BeanUtil.BASE_DATA_TYPE
     *
     * @param fieldName 属性名称
     * @param val 属性值
     * @param excludeIds 排除id数组
     * @return
     */
    public boolean isUnique(String fieldName,Object val,K ... excludeIds){
        boolean flag = true;
        List<T> resultList = repository.findAll((Root<T> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder)-> {
            {
                Predicate predicate = criteriaBuilder.conjunction();
                List<Expression<Boolean>> expressions = predicate.getExpressions();
                expressions.add(criteriaBuilder.equal(root.get(fieldName),val));
                return predicate;
            }
        });
        if(resultList==null||resultList.size()==0){
            return true;
        }else{
            if(excludeIds==null||excludeIds.length==0||Arrays.stream(excludeIds).filter(id->id!=null).count()==0) {
                return false;
            }else{
                Set<K> idSet= Arrays.stream(excludeIds).filter(id->id!=null).collect(Collectors.toSet());
                List filterList=resultList.stream().filter(e->!idSet.contains(RDBUtil.getPKVal(e))).collect(Collectors.toList());
                if (filterList!=null&&filterList.size()>0){
                    flag= false;
                }
            }
        }
        return flag;
    }


    /**
     * 获取唯一注解字段的message值
     * @param field
     * @return
     */
    private String getUniqueMessage(Field field){
        Unique anno= field.getAnnotation(Unique.class);
        String msg=anno.messageValue();
        if(StringUtils.isEmpty(msg)){
            msg= I18NData.getI18NData(anno.messageKey()).getValue(field.getName());
        }
        return msg;
    }

    /**
     * 保存前进行唯一性验证
     * @param t
     */
    public void validateUniqueBeforeSave(T t){
        if(!getBeanInfo().isCheckUnique){
            return;
        }
        //1、循环集合,验证每个唯一字段是否在数据库中有重复值
        for (Field f : getBeanInfo().uniqueFieldList) {
            Object val;
            try {
                val = PropertyUtils.getProperty(t,f.getName());
            }  catch (IllegalAccessException |InvocationTargetException |NoSuchMethodException e) {
                throw BaseRuntimeException.getException(e);
            }
            if(!isUnique(f.getName(),val,(K) RDBUtil.getPKVal(t))){
                throw BaseRuntimeException.getException(getUniqueMessage(f));
            }
        }
    }

    /**
     * 保存前进行批量唯一性验证
     * @param iterable
     */
    public void validateUniqueBeforeSave(Iterable<T> iterable){
        if(!getBeanInfo().isCheckUnique){
            return;
        }
        try {
            //1、循环集合,看传入的参数集合中唯一字段是否有重复的值
            Map<String,Set<Object>> fieldValueSetMap=new HashMap<>();
            for (T t : iterable) {
                for (Field f : getBeanInfo().uniqueFieldList) {
                    String fieldName=f.getName();
                    Object val=  PropertyUtils.getProperty(t,fieldName);
                    Set<Object> valueSet= fieldValueSetMap.get(fieldName);
                    if(valueSet==null){
                        valueSet=new HashSet<>();
                        fieldValueSetMap.put(fieldName,valueSet);
                    }else{
                        if(valueSet.contains(val)){
                            throw BaseRuntimeException.getException(getUniqueMessage(f));
                        }
                    }
                    valueSet.add(val);
                }
            }
            //2、循环集合,验证每个唯一字段是否在数据库中有重复值
            for (T t : iterable) {
                for (Field f : getBeanInfo().uniqueFieldList) {
                    String fieldName=f.getName();
                    Object val=PropertyUtils.getProperty(t,fieldName);
                    if(!isUnique(f.getName(),val,(K)RDBUtil.getPKVal(t))){
                        throw BaseRuntimeException.getException(getUniqueMessage(f));
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException| NoSuchMethodException e) {
            throw BaseRuntimeException.getException(e);
        }
    }
}
