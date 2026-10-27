package hwswbuilder.command;

/**
 * Taken from
 * https://www.programcreek.com/2014/06/leetcode-next-permutation-java/
 */
class NextPermutation {
    static void nextPermutation(int[] nums) {
        //find first decreasing digit
        int mark = -1;
        for (int i = nums.length - 1; i > 0; i--) {
            if (nums[i] > nums[i - 1]) {
                mark = i - 1;
                break;
            }
        }

        if (mark == -1) {
            reverse(nums, 0, nums.length - 1);
            return;
        }

        int idx = nums.length - 1;
        for (int i = nums.length - 1; i >= mark+1; i--) {
            if (nums[i] > nums[mark]) {
                idx = i;
                break;
            }
        }

        swap(nums, mark, idx);
        reverse(nums, mark + 1, nums.length - 1);
    }

    static void swap(int[] nums, int i, int j) {
        int t = nums[i];
        nums[i] = nums[j];
        nums[j] = t;
    }

    static void reverse(int[] nums, int i, int j) {
        while (i < j) {
            swap(nums, i, j);
            i++;
            j--;
        }
    }
}
