package android.icu.lang;

import android.icu.impl.UCharacterName;
import android.icu.util.ValueIterator;

class UCharacterNameIterator implements ValueIterator {
    private int m_choice_;
    private int m_current_;
    private int m_limit_;
    private UCharacterName m_name_;
    private int m_start_;
    private static char[] GROUP_OFFSETS_ = new char[33];
    private static char[] GROUP_LENGTHS_ = new char[33];
    private int m_groupIndex_ = -1;
    private int m_algorithmIndex_ = -1;

    @Override
    public boolean next(ValueIterator.Element element) {
        int algorithmLength;
        if (this.m_current_ >= this.m_limit_) {
            return false;
        }
        if ((this.m_choice_ == 0 || this.m_choice_ == 2) && this.m_algorithmIndex_ < (algorithmLength = this.m_name_.getAlgorithmLength())) {
            while (this.m_algorithmIndex_ < algorithmLength && (this.m_algorithmIndex_ < 0 || this.m_name_.getAlgorithmEnd(this.m_algorithmIndex_) < this.m_current_)) {
                this.m_algorithmIndex_++;
            }
            if (this.m_algorithmIndex_ < algorithmLength) {
                int algorithmStart = this.m_name_.getAlgorithmStart(this.m_algorithmIndex_);
                if (this.m_current_ < algorithmStart) {
                    if (this.m_limit_ <= algorithmStart) {
                        algorithmStart = this.m_limit_;
                    }
                    if (!iterateGroup(element, algorithmStart)) {
                        this.m_current_++;
                        return true;
                    }
                }
                if (this.m_current_ >= this.m_limit_) {
                    return false;
                }
                element.integer = this.m_current_;
                element.value = this.m_name_.getAlgorithmName(this.m_algorithmIndex_, this.m_current_);
                this.m_groupIndex_ = -1;
                this.m_current_++;
                return true;
            }
        }
        if (!iterateGroup(element, this.m_limit_)) {
            this.m_current_++;
            return true;
        }
        if (this.m_choice_ != 2 || iterateExtended(element, this.m_limit_)) {
            return false;
        }
        this.m_current_++;
        return true;
    }

    @Override
    public void reset() {
        this.m_current_ = this.m_start_;
        this.m_groupIndex_ = -1;
        this.m_algorithmIndex_ = -1;
    }

    @Override
    public void setRange(int i, int i2) {
        if (i >= i2) {
            throw new IllegalArgumentException("start or limit has to be valid Unicode codepoints and start < limit");
        }
        if (i < 0) {
            this.m_start_ = 0;
        } else {
            this.m_start_ = i;
        }
        if (i2 > 1114112) {
            this.m_limit_ = 1114112;
        } else {
            this.m_limit_ = i2;
        }
        this.m_current_ = this.m_start_;
    }

    protected UCharacterNameIterator(UCharacterName uCharacterName, int i) {
        if (uCharacterName == null) {
            throw new IllegalArgumentException("UCharacterName name argument cannot be null. Missing unames.icu?");
        }
        this.m_name_ = uCharacterName;
        this.m_choice_ = i;
        this.m_start_ = 0;
        this.m_limit_ = 1114112;
        this.m_current_ = this.m_start_;
    }

    private boolean iterateSingleGroup(ValueIterator.Element element, int i) {
        synchronized (GROUP_OFFSETS_) {
            synchronized (GROUP_LENGTHS_) {
                int groupLengths = this.m_name_.getGroupLengths(this.m_groupIndex_, GROUP_OFFSETS_, GROUP_LENGTHS_);
                while (this.m_current_ < i) {
                    int groupOffset = UCharacterName.getGroupOffset(this.m_current_);
                    String groupName = this.m_name_.getGroupName(GROUP_OFFSETS_[groupOffset] + groupLengths, GROUP_LENGTHS_[groupOffset], this.m_choice_);
                    if ((groupName == null || groupName.length() == 0) && this.m_choice_ == 2) {
                        groupName = this.m_name_.getExtendedName(this.m_current_);
                    }
                    if (groupName != null && groupName.length() > 0) {
                        element.integer = this.m_current_;
                        element.value = groupName;
                        return false;
                    }
                    this.m_current_++;
                }
                return true;
            }
        }
    }

    private boolean iterateGroup(ValueIterator.Element element, int i) {
        if (this.m_groupIndex_ < 0) {
            this.m_groupIndex_ = this.m_name_.getGroup(this.m_current_);
        }
        while (this.m_groupIndex_ < this.m_name_.m_groupcount_ && this.m_current_ < i) {
            int codepointMSB = UCharacterName.getCodepointMSB(this.m_current_);
            int groupMSB = this.m_name_.getGroupMSB(this.m_groupIndex_);
            if (codepointMSB == groupMSB) {
                if (codepointMSB == UCharacterName.getCodepointMSB(i - 1)) {
                    return iterateSingleGroup(element, i);
                }
                if (!iterateSingleGroup(element, UCharacterName.getGroupLimit(groupMSB))) {
                    return false;
                }
                this.m_groupIndex_++;
            } else if (codepointMSB > groupMSB) {
                this.m_groupIndex_++;
            } else {
                int groupMin = UCharacterName.getGroupMin(groupMSB);
                if (groupMin > i) {
                    groupMin = i;
                }
                if (this.m_choice_ == 2 && !iterateExtended(element, groupMin)) {
                    return false;
                }
                this.m_current_ = groupMin;
            }
        }
        return true;
    }

    private boolean iterateExtended(ValueIterator.Element element, int i) {
        while (this.m_current_ < i) {
            String extendedOr10Name = this.m_name_.getExtendedOr10Name(this.m_current_);
            if (extendedOr10Name != null && extendedOr10Name.length() > 0) {
                element.integer = this.m_current_;
                element.value = extendedOr10Name;
                return false;
            }
            this.m_current_++;
        }
        return true;
    }
}
