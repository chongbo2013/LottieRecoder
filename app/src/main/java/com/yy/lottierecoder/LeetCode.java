package com.yy.lottierecoder;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class LeetCode {


        public List<List<Integer>> threeSum(int[] nums) {
            List<List<Integer>> fullArs=new ArrayList<>();
            Set<String> strs= new HashSet<String>();
            int count=nums.length;
            Arrays.sort(nums);

            for(int i=0;i<count;i++){
                int i1=nums[i];

                for(int j=0;j<count;j++){
                    int j1=nums[j];
                    if(i==j){
                        continue;
                    }

                    for(int k=0;k<count;k++){
                        int k1=nums[k];
                        if(k==i||k==j){
                            continue;
                        }
                        if(i1+j1+k1==0){
                            List<Integer> fullArsResult=new ArrayList();
                            String str=i1+","+j1+","+k1;
                            if(strs.contains(str)){
                                continue;
                            }
                            strs.add(str);
                            fullArsResult.add(i1);
                            fullArsResult.add(j1);
                            fullArsResult.add(k1);
                            fullArs.add(fullArsResult);
                        }

                    }


                }

            }
            return fullArs;
        }

}
